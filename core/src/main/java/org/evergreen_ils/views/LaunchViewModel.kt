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
import com.android.volley.Request
import com.android.volley.Response
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.evergreen_ils.Api
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Log
import org.opensrf.util.GatewayResponse
import java.lang.Exception
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "LaunchViewModel"

class LaunchViewModel : ViewModel() {

    private val data: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            fetchData()
        }
    }

    fun  getData(): LiveData<String> {
        return data
    }

    private fun fetchData() {
        viewModelScope.launch {
            try {
                var now_ms = System.currentTimeMillis()

                // this part is serial
                Log.d(TAG, "coro: 1: start")
                val serverVersion = getServerVersion()
                now_ms = Log.logElapsedTime(TAG, now_ms,"coro: 1")
                data.value = serverVersion;

                // parallel
                var defs = arrayListOf<Deferred<Any>>()
                Log.d(TAG, "coro: 2: serverVersion:$serverVersion")
                val settings = arrayListOf(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
                        Api.SETTING_CREDIT_PAYMENTS_ALLOW)
                for (orgID in 1..340) {
                    val args = arrayOf<Any>(orgID, settings, Api.ANONYMOUS)
                    val def = async {
                        makeRequest<String>(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args) { response ->
                            Log.d(TAG, "coro: gateway_response:$response")
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
            }
        }
    }

    private suspend fun getServerVersion() = suspendCoroutine<String> { cont ->
        //val url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
        val url = Gateway.buildUrl(
                Api.ACTOR, Api.ILS_VERSION,
                arrayOf())
        val start_ms = System.currentTimeMillis()
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                Response.Listener { response ->
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

    private suspend fun <T> makeRequest(service: String, method: String, args: Array<Any>, block: (GatewayResponse) -> T?) = suspendCoroutine<T> { cont ->
        val url = Gateway.buildUrl(service, method, args)
        //Log.d(TAG, "coro: url:$url")
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                Response.Listener { response ->
                    val res = block(response)
                    if (res == null) {
                        cont.resumeWithException(GatewayError("Unexpected response"))
                    } else {
                        cont.resumeWith(Result.success(res))
                    }
                },
                Response.ErrorListener { error ->
                    cont.resumeWithException(error)
                })
        VolleyWrangler.getInstance().addToRequestQueue(r)
    }
}