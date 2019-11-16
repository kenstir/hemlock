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

package org.evergreen_ils.net

import com.android.volley.Request
import com.android.volley.Response
import kotlinx.coroutines.CoroutineScope
import org.evergreen_ils.android.App
import org.evergreen_ils.system.Utils
import org.opensrf.util.GatewayResponse
import java.lang.Exception
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class GatewayState {
    UNINITIALIZED,
    INITIALIZED,
    LOADED
}

object Gateway {
    val baseUrl: String?
        get() = App.getLibrary()?.url ?: "https://kenstir.ddns.net" // total hack-o-rama
    val scope: CoroutineScope? = null

    var state: GatewayState = GatewayState.UNINITIALIZED

    fun buildUrl(service: String, method: String, args: Array<Any>): String? {
        var url = baseUrl?.plus(Utils.buildGatewayUrl(service, method, args))
        return url
    }

    // Wrap an OSRF Gateway request in a coroutine.  `block` is expected to return T or throw
    suspend fun <T> makeRequest(service: String, method: String, args: Array<Any>, block: (GatewayResponse) -> T) = suspendCoroutine<T> { cont ->
        val url = buildUrl(service, method, args)
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                Response.Listener { response ->
                    try {
                        val res = block(response)
                        cont.resumeWith(Result.success(res))
//                        if (res == null) {
//                            cont.resumeWithException(GatewayError("Unexpected response"))
//                        } else {
//                            cont.resumeWith(Result.success(res))
//                        }
                    } catch (ex: Exception) {
                        cont.resumeWithException(ex)
                    }
                },
                Response.ErrorListener { error ->
                    cont.resumeWithException(error)
                })
        VolleyWrangler.getInstance().addToRequestQueue(r)
    }
}