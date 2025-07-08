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
import net.kenstir.ui.Analytics
import net.kenstir.ui.util.showAlert
import org.evergreen_ils.gateway.GatewayClient
import java.util.Collections

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
    private var start_ms: Long = 0

    override fun setContentViewImpl() {
        setContentView(R.layout.activity_generic_login)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.initialize(this)

        directoryUrl = getString(R.string.evergreen_libraries_url)

        librarySpinner = findViewById(R.id.choose_library_spinner)
        librarySpinner?.setOnItemSelectedListener(object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                setLibrary(libraries[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setLibrary(null)
            }
        })
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
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()

                val url = directoryUrl ?: return@async
                val client = GatewayClient.client
                val json = client.get(url).bodyAsText()
                loadLibrariesFromJson(json)
                onLibrariesLoaded()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            }
        }
    }

    private fun chooseNearestLibrary() {
        var location: Location? = null
        val lm = getSystemService(LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
            try {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (ex: SecurityException) {
            }
        }

        var min_distance = Float.MAX_VALUE
        var default_library_index: Int? = null
        for (i in libraries.indices) {
            val library = libraries[i]
            if (location != null && library.location != null) {
                val distance = location.distanceTo(library.location)
                if (distance < min_distance) {
                    default_library_index = i
                    min_distance = distance
                }
            }
        }
        if (default_library_index != null) {
            librarySpinner!!.setSelection(default_library_index)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            // we could show an expanation to the user here, but it really is not worth it
            return
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_COARSE_LOCATION)
        }
    }

    private fun onLibrariesLoaded() {
        // if the user has any existing accounts, then we can select a reasonable default library
        var default_library: Library? = null
        val existing_accounts = net.kenstir.ui.account.AccountUtils.getAccountsByType(this@GenericAuthenticatorActivity)
        Log.d(net.kenstir.ui.account.Const.AUTH_TAG, "there are " + existing_accounts.size + " existing accounts")
        if (existing_accounts.size > 0) {
            default_library = net.kenstir.ui.account.AccountUtils.getLibraryForAccount(this@GenericAuthenticatorActivity,
                existing_accounts[0])
            Log.d(net.kenstir.ui.account.Const.AUTH_TAG,
                "default_library=$default_library")
        }

        // Build a List<String> for use in the spinner adapter
        // While we're at it choose a default library; first by prior account, second by proximity
        var default_library_index: Int? = null
        val l = ArrayList<String?>(libraries.size)
        for ((url, _, directoryName) in libraries) {
            if (default_library != null && TextUtils.equals(default_library.url, url)) {
                default_library_index = l.size
            }
            l.add(directoryName)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, l)
        librarySpinner!!.adapter = adapter
        if (default_library_index != null) {
            librarySpinner!!.setSelection(default_library_index)
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

        Collections.sort(libraries) { a, b -> a.directoryName!!.compareTo(b.directoryName!!) }
    }

    companion object {
        private val TAG: String = GenericAuthenticatorActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST_COARSE_LOCATION = 1
    }
}
