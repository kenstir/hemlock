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

package org.evergreen_ils.views.launch

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.App
import org.evergreen_ils.net.Gateway
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.data.evergreen.XGatewayClient
import org.evergreen_ils.system.*
import org.evergreen_ils.utils.getCustomMessage
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "LaunchViewModel"

class LaunchViewModel : ViewModel() {

    private var errors = AtomicInteger(0)

    private val _status = MutableLiveData<String>()
    val status: LiveData<String>
        get() = _status

    private val _spinner = MutableLiveData<Boolean>()
    val spinner: LiveData<Boolean>
        get() = _spinner

    private val _serviceDataReady = MutableLiveData<Boolean>()
    val serviceDataReady: LiveData<Boolean>
        get() = _serviceDataReady

    private fun onLoadError(ex: Exception) {
        errors.incrementAndGet()
        Log.d(TAG, "ex:$ex")
        try {
            _status.value = ex.getCustomMessage()
        } catch (ex: java.lang.Exception) {
            _status.value = "Unexpected error"
        }
        _serviceDataReady.value = false
    }

    // This is where most global initialization happens: especially loading the IDL necessary
    // for decoding most gateway responses.  Important notes:
    // * server version and IDL are done synchronously and first
    // * other initialization can happen async after that
    fun loadServiceData(resources: Resources) {
        viewModelScope.async {
            val outerJob = viewModelScope.async {
                // update the UI
                _spinner.value = true
                _status.value = "Connecting to server"
                errors.set(0)

                val start_ms = System.currentTimeMillis()
                var now_ms = start_ms

                // sync: serverVersion is a key for caching all other requests
                val serverVersion = when (val result = App.getServiceConfig().authService.fetchServerVersion()) {
                    is Result.Success -> { result.data }
                    is Result.Error -> { onLoadError(result.exception) ; return@async }
                }
                XGatewayClient.serverCacheKey = serverVersion
                Gateway.serverCacheKey = serverVersion
                now_ms = Log.logElapsedTime(TAG, now_ms, "fetchServerVersion: $serverVersion")

                // sync: serverCacheKey is an additional way for the EG admin to clear the app cache
                val serverCacheKey = when (val result = App.getServiceConfig().authService.fetchServerCacheKey()) {
                    is Result.Success -> { result.data }
                    is Result.Error -> { onLoadError(result.exception) ; return@async }
                }
                serverCacheKey?.let {
                    XGatewayClient.serverCacheKey = "$serverVersion-$serverCacheKey"
                    Gateway.serverCacheKey = "$serverVersion-$serverCacheKey"
                }
                now_ms = Log.logElapsedTime(TAG, now_ms, "fetchServerCacheKey: $serverCacheKey")

                // sync: load IDL
                EgIDL.loadIDL()
                now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL")

                // sync: load messages
                EgMessageMap.init(resources)

                // ---------------------------------------------------------------
                // We could move this init until later, as is done for iOS
                // but this more closely models the existing Android app
                // ---------------------------------------------------------------

                var defs = arrayListOf<Deferred<Any>>()
                defs.add(viewModelScope.async {
                    val result = Gateway.actor.fetchOrgTypes()
                    when (result) {
                        is Result.Success -> EgOrg.loadOrgTypes(result.data)
                        is Result.Error -> onLoadError(result.exception)
                    }
                })
                defs.add(viewModelScope.async {
                    val result = Gateway.actor.fetchOrgTree()
                    when (result) {
                        is Result.Success -> EgOrg.loadOrgs(result.data, resources.getBoolean(R.bool.ou_hierarchical_org_tree))
                        is Result.Error -> onLoadError(result.exception)
                    }
                })
                defs.add(viewModelScope.async {
                    val result = Gateway.search.fetchCopyStatuses()
                    when (result) {
                        is Result.Success -> EgCopyStatus.loadCopyStatuses(result.data)
                        is Result.Error -> onLoadError(result.exception)
                    }
                })
                defs.add(viewModelScope.async {
                    val result = Gateway.pcrud.fetchCodedValueMaps()
                    when (result) {
                        is Result.Success -> EgCodedValueMap.loadCodedValueMaps(result.data)
                        is Result.Error -> onLoadError(result.exception)
                    }
                })

                // await all deferreds (see awaitAll doc for differences)
                Log.d(TAG, "[kcxxx] await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "[kcxxx] await ${defs.size} deferreds ... done")

                if (errors.get() == 0) {
                    _status.value = "Connected"
                    _serviceDataReady.value = true
                }
                Log.logElapsedTime(TAG, start_ms,"total")
            }
            try {
                outerJob.await()
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] caught in loadServiceData", ex)
                _status.value = ex.message
                _serviceDataReady.value = false
            } finally {
                _spinner.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
    }
}
