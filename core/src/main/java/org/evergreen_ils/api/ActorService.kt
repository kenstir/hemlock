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

package org.evergreen_ils.api

import com.android.volley.Request
import com.android.volley.Response
import org.evergreen_ils.Api
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Log
import kotlin.coroutines.resumeWithException

private const val TAG = "api"

class ActorService {
    fun asdf(): String {
        return "x"
    }
//    suspend fun fetchServerVersion() -> suspendCoroutine<String> { cont ->
////        val url = Gateway.makeUrl(Api.ACTOR, Api.ILS_VERSION,
////                arrayOf()))
//        val url = "https://bark.cwmars.org/osrf-gateway-v1?service=open-ils.actor&method=opensrf.open-ils.system.ils_version"
//        val start_ms = System.currentTimeMillis()
//        val r = GatewayJsonObjectRequest(
//                url,
//                Request.Priority.NORMAL,
//                Response.Listener { response ->
//                    val duration_ms = System.currentTimeMillis() - start_ms
//                    val ver = response.payload as String
//                    Log.d(TAG, "coro: listener, resp:$ver")
//                    cont.resumeWith(Result.success(ver))
//                },
//                Response.ErrorListener { error ->
//                    Log.d(TAG, "caught", error)
//                    cont.resumeWithException(error)
//                })
//        VolleyWrangler.getInstance(this).addToRequestQueue(r)
//    }

}