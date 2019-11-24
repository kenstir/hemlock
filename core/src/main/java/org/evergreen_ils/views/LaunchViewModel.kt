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

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.evergreen_ils.Api
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.api.ActorService
import org.evergreen_ils.api.EvergreenService
import org.evergreen_ils.api.PCRUDService
import org.evergreen_ils.api.SearchService
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.searchCatalog.CodedValueMap
import org.evergreen_ils.system.Account
import org.evergreen_ils.system.Log
import org.opensrf.util.GatewayResponse

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
    fun loadServiceData(account: Account) {
        if (account.authToken.isNullOrEmpty())
            return
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
                defs.add(async { EvergreenService.loadOrgs(ActorService.fetchOrgTree(), App.getApplicationContext().resources.getBoolean(R.bool.ou_hierarchical_org_tree)) })
                //unused defs.add(async { EvergreenService.loadCopyStatuses(SearchService.fetchCopyStatuses()) })
                defs.add(async { CodedValueMap.loadCodedValueMaps(PCRUDService.fetchCodedValueMaps()) })

                // awaitAll
                Log.d(TAG, "coro: 3: await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "coro: 4: await ${defs.size} deferreds ... done")

                _status.value = "Connected"
                _serviceDataReady.value = true
                Log.logElapsedTime(TAG, start_ms,"total")
            }
            try {
                outerJob.await()
            } catch (ex: Exception) {
                Log.d(TAG, "caught", ex)
                _status.value = ex.message
            } finally {
                _spinner.value = false
            }
        }
    }

    fun loadAccountData(account: Account) {
        if (account.authToken.isNullOrEmpty())
            return
        viewModelScope.async {
            val outerJob = async {
                _spinner.value = true
                _status.value = "Retrieving user settings"

                // TODO: fetch session
                // TODO: verify that session is OK
//                var sessionJob = async { AuthService.fetchSession() }

                // TODO: load bookbags
                //async { ActorService.fetchBookbags() }

                _accountDataReady.value = true
            }
            try {
                outerJob.await()
            } catch (ex: Exception) {
                Log.d(TAG, "caught", ex)
                _status.value = ex.message
            } finally {
                _spinner.value = false
            }
        }
    }

    /*
val settings = arrayListOf(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
        Api.SETTING_CREDIT_PAYMENTS_ALLOW)
for (orgID in 1..50) {
    val def = async {
        Log.d(TAG, "org:$orgID settings ... ")
        val settingsMap = ActorService.fetchOrgSettings(orgID)
        val isNotPickupLib = ActorService.parseBoolSetting(settingsMap, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        val areCreditPaymentsAllowed = ActorService.parseBoolSetting(settingsMap, Api.SETTING_CREDIT_PAYMENTS_ALLOW)
        Dispatchers.Main {
            Log.d(TAG, "org:$orgID settings ... fetched $isNotPickupLib $areCreditPaymentsAllowed")
            //TODO: Organization.set(orgID, isNotPickupLib, areCreditPaymentsAllowed)
        }
        "xyzzy"
    }
    defs.add(def)
}
now_ms = Log.logElapsedTime(TAG, now_ms,"coro: 2")
*/

    // response.payload looks like:
    // {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
    private fun parseBoolSetting(response: GatewayResponse, setting: String): Boolean? {
        var value: Boolean? = null
        val map = response.payload as? Map<String, Any>
        Log.d(TAG, "map:$map")
        if (map != null) {
            val o = map[setting]
            if (o != null) {
                val setting_map = o as? Map<String, *>
                if (setting_map != null) {
                    value = Api.parseBoolean(setting_map["value"])
                }
            }
        }
        return value
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "hey lookit this")
    }
}