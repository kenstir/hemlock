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
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Account
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.Log
import org.opensrf.util.GatewayResponse
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

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    fun fetchData(account: Account) {
        if (account.authToken.isNullOrEmpty())
            return
        viewModelScope.launch {
            try {
                _spinner.value = true
                var now_ms = System.currentTimeMillis()

                // FAKE NEWS
                _status.value = "Connecting to server"
                Log.d(TAG, "set _account 1")
                _account.value = account
                delay(1_500)

                // FAKE NEWS
                _status.value = "Starting session"
                Log.d(TAG, "set _account 2")
                _account.value = account
                delay(1_500)

                // FAKE NEWS
                Log.d(TAG, "set _account 3")
                _account.value = account

                // get server version
//                val serverVersion = fetchServerVersion()

                // then launch a bunch of requests in parallel
                var defs = arrayListOf<Deferred<Any>>()
                Log.d(TAG, "coro: 2: start")
                val settings = arrayListOf(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
                        Api.SETTING_CREDIT_PAYMENTS_ALLOW)
                for (orgID in 49..50) {
                    val args = arrayOf<Any>(orgID, settings, account.authToken)
                    val def = async {
                        Gateway.makeRequest<String>(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args) { response ->
                            //Log.d(TAG, "coro: gateway_response:$response")
                            val x = parseBoolSetting(response, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
                            val y = parseBoolSetting(response, Api.SETTING_CREDIT_PAYMENTS_ALLOW)
                            "xyzzy"
                        }
                    }
                    defs.add(def)
                }
                now_ms = Log.logElapsedTime(TAG, now_ms,"coro: 2")

                // awaitAll
                Log.d(TAG, "coro: 3: await ${defs.size} deferreds ...")
                defs.map { it.await() }
                Log.d(TAG, "coro: 4: await ${defs.size} deferreds ... done")
                Log.logElapsedTime(TAG, now_ms,"coro: 4")
            } catch (ex: Exception) {
                Log.d(TAG, "caught", ex)
                _status.value = ex.message
            } finally {
                _spinner.value = false
            }
        }
    }

    private fun parseBoolSetting(response: GatewayResponse, setting: String): Boolean? {
        var value: Boolean? = null
        val map = response.payload as? Map<String, Any>
        Log.d(TAG, "map:$map")
        if (map != null) {
            val o = map[setting]
            if (o != null) {
                val setting_map = o as Map<String, *>
                value = Api.parseBoolean(setting_map["value"])
            }
        }
        return value
    }

    suspend fun fetchServerVersion(): String = withContext(Dispatchers.IO) {
        Gateway.makeRequest(Api.ACTOR, Api.ILS_VERSION, arrayOf()) { response ->
            response.payload as String
        }
    }

    private suspend fun getServerVersion() = suspendCoroutine<String> { cont ->
        Log.d(TAG, "coro: getServerVersion: start")
        val url = Gateway.buildUrl(
                Api.ACTOR, Api.ILS_VERSION,
                arrayOf())
        val start_ms = System.currentTimeMillis()
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                Response.Listener { response ->
                    Log.d(TAG, "coro: getServerVersion: response")
                    val ver = response.payload as String
                    Log.d(TAG, "coro: listener, resp:$ver")
                    Log.logElapsedTime(TAG, start_ms, "coro: listener")
                    cont.resumeWith(Result.success(ver))
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, "caught", error)
                    cont.resumeWithException(error)
                })
        VolleyWrangler.getInstance().addToRequestQueue(r)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }
}