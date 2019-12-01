/*
 * Copyright (c) 2019 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.views

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.api.ActorService
import org.evergreen_ils.api.EvergreenService
import org.evergreen_ils.api.PCRUDService
import org.evergreen_ils.api.SearchService
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.searchCatalog.CodedValueMap
import org.evergreen_ils.system.Log

private const val TAG = "LaunchViewModel"

class LaunchViewModel : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String>
        get() = _status

    private val _spinner = MutableLiveData<Boolean>()
    val spinner: LiveData<Boolean>
        get() = _spinner

    private val _serviceDataReady = MutableLiveData<Boolean>()
    val serviceDataReady: LiveData<Boolean>
        get() = _serviceDataReady

    private val _accountDataReady = MutableLiveData<Boolean>()
    val accountDataReady: LiveData<Boolean>
        get() = _accountDataReady

    // This is where most global initialization happens: especially loading the IDL necessary
    // for decoding most gateway responses.  Important notes:
    // * server version and IDL are done synchronously and first
    // * other initialization can happen async after that
    fun loadServiceData(resources: Resources) {
        viewModelScope.async {
            val outerJob = async {
                // update the UI
                _spinner.value = true
                _status.value = "Connecting to server"

                val start_ms = System.currentTimeMillis()
                var now_ms = start_ms

                // sync: serverVersion is a key for caching all other requests
                Gateway.serverCacheKey = ActorService.fetchServerVersion()
                now_ms = Log.logElapsedTime(TAG, now_ms, "fetchServerVersion")

                // sync: load IDL
                EvergreenService.loadIDL()
                now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL")

                // ---------------------------------------------------------------
                // We could move this init until later, as is done for iOS
                // but this more closely models the existing Android app
                // ---------------------------------------------------------------

                var defs = arrayListOf<Deferred<Any>>()
                defs.add(async { EvergreenService.loadOrgTypes(ActorService.fetchOrgTypes()) })
                defs.add(async { EvergreenService.loadOrgs(ActorService.fetchOrgTree(), resources.getBoolean(R.bool.ou_hierarchical_org_tree)) })
                defs.add(async { EvergreenService.loadCopyStatuses(SearchService.fetchCopyStatuses()) })
                defs.add(async { CodedValueMap.loadCodedValueMaps(PCRUDService.fetchCodedValueMaps()) })

                // awaitAll
                Log.d(TAG, "coro: await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "coro: await ${defs.size} deferreds ... done")

                _status.value = "Connected"
                _serviceDataReady.value = true
                Log.logElapsedTime(TAG, start_ms,"total")
            }
            try {
                outerJob.await()
            } catch (ex: Exception) {
                Log.d(TAG, "caught in loadServiceData", ex)
                _status.value = ex.message
                _serviceDataReady.value = false
            } finally {
                _spinner.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }
}
