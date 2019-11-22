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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.evergreen_ils.Api
import org.evergreen_ils.api.ActorService
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.system.Account
import org.evergreen_ils.system.EvergreenServer
import org.evergreen_ils.system.Log
import org.open_ils.idl.IDLParser
import org.opensrf.util.GatewayResponse

private const val TAG = "LaunchViewModel"

class LaunchViewModel : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String>
        get() = _status

    private val _spinner = MutableLiveData<Boolean>()
    val spinner: LiveData<Boolean>
        get() = _spinner

    private val _readyPlayerOne = MutableLiveData<Boolean>()
    val readyPlayerOne: LiveData<Boolean>
        get() = _readyPlayerOne

    fun fetchData(account: Account) {
        if (account.authToken.isNullOrEmpty())
            return
        viewModelScope.async {
            val def = async {
                // update the UI
                _spinner.value = true
                _status.value = "Connecting to server"

                val start_ms = System.currentTimeMillis()
                var now_ms = start_ms

                // use serverVersion as cache-busting arg
                val serverVersion = ActorService.fetchServerVersion()
                now_ms = Log.logElapsedTime(TAG, now_ms, "fetchServerVersion")

                // load IDL
                val url = EvergreenServer.getIDLUrl(Gateway.baseUrl, serverVersion)
                val xml = Gateway.makeStringRequest(url)
                val parser = IDLParser(xml.byteInputStream())
                Log.logElapsedTime(TAG, now_ms, "loadIDL.get")
                parser.parse()
                now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL.total")

                // ---------------------------------------------------------------
                // We could move this init until later, as is done for iOS
                // but this more closely models the existing Android app
                // ---------------------------------------------------------------

                var defs = arrayListOf<Deferred<Any>>()

                // load Orgs
                val orgsDeferred = async {

                    val orgTypes = ActorService.fetchOrgTypes()
                    Log.d(TAG, "orgTypes:$orgTypes")
//                    val orgs = ActorService.fetchOrgTree()
//                    Log.d(TAG, "orgs:$orgs")
                    Log.d(TAG, "hmmm")
                }
                defs.add(orgsDeferred)

                /*
                // then launch a bunch of requests in parallel
                Log.d(TAG, "coro: 2: start")
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

                // awaitAll
                Log.d(TAG, "coro: 3: await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "coro: 4: await ${defs.size} deferreds ... done")

                _status.value = "passcode secured"
                _readyPlayerOne.value = true
                Log.logElapsedTime(TAG, start_ms,"total")
            }
            try {
                def.await()
            } catch (ex: Exception) {
                Log.d(TAG, "caught", ex)
                _status.value = ex.message
            } finally {
                _spinner.value = false
            }
        }
    }

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
}