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

import android.accounts.AccountManager
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.Response
import kotlinx.coroutines.*
import org.evergreen_ils.Api
import org.evergreen_ils.accountAccess.AccountUtils
import org.evergreen_ils.api.ActorService
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.*
import org.open_ils.idl.IDLParser
import org.opensrf.util.GatewayResponse
import java.net.URL
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
        viewModelScope.launch {
            try {
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
                /*
                Dispatchers.IO {
                    Log.d(TAG, "fetch IDL from $url")
                    Log.logElapsedTime(TAG, now_ms, "loadIDL.init")
                    URL(url).openStream().use {
                        val parser = IDLParser(it)
                        Log.logElapsedTime(TAG, now_ms, "loadIDL.openStream")
                        parser.parse()
                        now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL.total")
                    }
                }
                 */

                // load IDL - faster with Volley due to caching
                Log.logElapsedTime(TAG, now_ms, "loadIDL.init")
                Gateway.makeStringRequest(url) {
                    val parser = IDLParser(it.byteInputStream())
                    Log.logElapsedTime(TAG, now_ms, "loadIDL.ctor")
                    parser.parse()
                    now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL.total")
                }

                /*
                // then launch a bunch of requests in parallel
                var defs = arrayListOf<Deferred<Any>>()
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

                // awaitAll
                Log.d(TAG, "coro: 3: await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "coro: 4: await ${defs.size} deferreds ... done")
*/

                _status.value = "passcode secured"
                _readyPlayerOne.value = true
                Log.logElapsedTime(TAG, start_ms,"coro: full monty")
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

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }
}