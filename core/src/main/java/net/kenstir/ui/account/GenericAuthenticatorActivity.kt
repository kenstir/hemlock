/*
 * Copyright (c) 2025 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */
package net.kenstir.ui.account

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kenstir.data.model.Library
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.util.Analytics
import net.kenstir.ui.util.showAlert
import org.evergreen_ils.gateway.GatewayClient

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class DirectoryEntry(
    @SerialName("short_name") val shortName: String,
    @SerialName("directory_name") val directoryName: String,
    val url: String,
    val latitude: Double,
    val longitude: Double,
)

class GenericAuthenticatorActivity: AuthenticatorActivity() {
    private var librarySpinner: Spinner? = null
    var libraries: MutableList<Library> = ArrayList()
    var directoryUrl: String? = null

    override fun setContentViewImpl() {
        setContentView(R.layout.activity_generic_login)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        directoryUrl = getString(R.string.evergreen_libraries_url)

        librarySpinner = findViewById(R.id.choose_library_spinner)
        librarySpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                setLibrary(libraries[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setLibrary(null)
            }
        }
    }

    override fun initSelectedLibrary() {
        setLibrary(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[fetch] fetchData ...")
                val start = System.currentTimeMillis()

                val url = directoryUrl ?: return@async
                val client = GatewayClient.client
                val json = client.get(url).bodyAsText()
                loadLibrariesFromJson(json)

                val existingAccounts = AccountUtils.getAccountsByType(this@GenericAuthenticatorActivity)
                val numAccountsByLibrary = mutableMapOf<Library, Int>()
                for (account in existingAccounts) {
                    val library = AccountUtils.getLibraryForAccountBlocking(
                        this@GenericAuthenticatorActivity, account.name, account.type)
                    val count = numAccountsByLibrary[library] ?: 0
                    numAccountsByLibrary[library] = count + 1
                }

                onDataLoaded(existingAccounts, numAccountsByLibrary)
                Log.logElapsedTime(TAG, start, "[fetch] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchData ... caught", ex)
                showAlert(ex)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun chooseNearestLibrary() {
        var location: Location? = null
        val lm = getSystemService(LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
            try {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                Log.d(TAG, "[auth] got location")
            } catch (ex: SecurityException) {
                Log.d(TAG, "[auth] failed to get location: $ex.message")
            }
        }

        var minDistance = Float.MAX_VALUE
        var defaultLibraryIndex: Int? = null
        for (i in libraries.indices) {
            val library = libraries[i]
            if (location != null && library.location != null) {
                val distance = location.distanceTo(library.location)
                if (distance < minDistance) {
                    defaultLibraryIndex = i
                    minDistance = distance
                }
            }
        }
        if (defaultLibraryIndex != null) {
            librarySpinner?.setSelection(defaultLibraryIndex)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseNearestLibrary()
            }
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            chooseNearestLibrary()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // we could show an explanation to the user here, but it really is not worth it
            return
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_COARSE_LOCATION)
        }
    }

    private fun onDataLoaded(existingAccounts: List<android.accounts.Account>,
                             numAccountsByLibrary: Map<Library, Int>) {
        // if the user has any existing accounts, then choose the library with the most existing accounts
        var defaultLibrary: Library? = null
        if (existingAccounts.isNotEmpty()) {
            var maxCount = 0
            for ((library, count) in numAccountsByLibrary) {
                if (count > maxCount) {
                    defaultLibrary = library
                    maxCount = count
                }
            }
            Log.d(Const.AUTH_TAG, "[auth] defaultLibrary=$defaultLibrary")
        }

        // Build a List<String> for use in the spinner adapter
        // While we're at it choose a default library; first by prior account, second by proximity
        var defaultLibraryIndex: Int? = null
        val l = ArrayList<String?>(libraries.size)
        for ((url, _, directoryName) in libraries) {
            if (defaultLibrary != null && TextUtils.equals(defaultLibrary.url, url)) {
                defaultLibraryIndex = l.size
            }
            l.add(directoryName)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, l)
        librarySpinner?.adapter = adapter
        if (defaultLibraryIndex != null) {
            librarySpinner?.setSelection(defaultLibraryIndex)
        } else {
            requestPermission()
        }
    }

    private fun loadLibrariesFromJson(json: String) {
        libraries.clear()

        if (Analytics.isDebuggable(this)) {
            libraries.add(Library("http://192.168.1.8", "debug catalog", "00debug catalog", null))
        }

        val entries = Json.decodeFromString<List<DirectoryEntry>>(json)
        for (entry in entries) {
            val location = Location("")
            location.latitude = entry.latitude
            location.longitude = entry.longitude
            val library = Library(entry.url, entry.shortName, entry.directoryName, location)
            libraries.add(library)
        }

        libraries.sortWith(Comparator { a, b -> a.directoryName!!.compareTo(b.directoryName!!) })
    }

    companion object {
        private const val TAG = "GenericAuthenticator"

        private const val PERMISSIONS_REQUEST_COARSE_LOCATION = 1
    }
}
