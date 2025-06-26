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

import android.net.Uri
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import net.kenstir.hemlock.net.RequestOptions
import org.evergreen_ils.Api
import net.kenstir.hemlock.logging.Log
import org.opensrf.net.http.HttpConnection
import org.opensrf.util.GatewayResult
import org.opensrf.util.JSONWriter
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

const val TAG = "Gateway"
private const val INITIAL_URL_SIZE = 128

/// For notes on caching, see docs/notes-on-caching.md
object Gateway {
    lateinit var baseUrl: String
    lateinit var clientCacheKey: String

    var actor: ActorService = GatewayActor
    var circ: CircService = GatewayCirc
    var fielder: FielderService = GatewayFielder
    var pcrud: PCRUDService = GatewayPCRUD
    var search: SearchService = GatewaySearch

    private var _serverCacheKey: String? = null
    private val startTime = System.currentTimeMillis()
    var serverCacheKey: String
        get() = _serverCacheKey ?: startTime.toString()
        set(value) { _serverCacheKey = value }

    var randomErrorPercentage = 0
    var defaultTimeoutMs = 30_000
    var searchTimeoutMs = 60_000
    const val limitedCacheTtlSeconds = 86_400 * 7 // 1 week

    fun buildQuery(service: String?, method: String?, params: Array<Any?>, addCacheArgs: Boolean = true): String {
        val sb = StringBuilder(INITIAL_URL_SIZE)
        sb.append("service=").append(service)
        sb.append("&method=").append(method)
        for (param in params) {
            sb.append("&param=")
            sb.append(Uri.encode(JSONWriter(param).write(), "UTF-8"))
        }

        if (addCacheArgs) {
            sb.append("&_ck=").append(clientCacheKey)
            sb.append("&_sk=").append(serverCacheKey)
        }

        // Add OG for Old Gateway code, so I can find it in the logs
        sb.append("&_v=OG")

        return sb.toString()
    }

    @JvmOverloads
    fun buildUrl(service: String, method: String, args: Array<Any?>, addCacheArgs: Boolean = true): String {
        return baseUrl.plus("/osrf-gateway-v1?").plus(
                buildQuery(service, method, args, addCacheArgs))
    }

    fun getUrl(relativeUrl: String): String {
        return baseUrl.plus(relativeUrl)
    }

    fun getIDLUrl(shouldCache: Boolean = true): String {
        val params = mutableListOf<String>()
        for (className in TextUtils.split(Api.IDL_CLASSES_USED, ",")) {
            params.add("class=$className")
        }
        if (shouldCache) {
            params.add("_ck=$clientCacheKey")
            params.add("_sk=$serverCacheKey")
        }
        return baseUrl.plus("/reports/fm_IDL.xml?")
                .plus(TextUtils.join("&", params))
    }

    // Make an OSRF Gateway request from inside a CoroutineScope.  `block` is expected to return T or throw
    suspend fun <T> fetch(service: String, method: String, args: Array<Any?>, options: RequestOptions, block: (GatewayResult) -> T) =
            fetchImpl(service, method, args, options, block)
    suspend fun <T> fetch(service: String, method: String, args: Array<Any?>, shouldCache: Boolean, block: (GatewayResult) -> T) =
            fetchImpl(service, method, args, RequestOptions(defaultTimeoutMs, shouldCache, true), block)

    private suspend fun <T> fetchImpl(service: String, method: String, args: Array<Any?>, options: RequestOptions, block: (GatewayResult) -> T) = suspendCoroutine<T> { cont ->
        maybeInjectRandomError()
        val url = buildUrl(service, method, args, options.shouldCache)
        val r = GatewayJsonRequest(
                url,
                Request.Priority.NORMAL,
                { response ->
                    try {
                        val res = block(response)
                        cont.resumeWith(Result.success(res))
                    } catch (ex: Exception) {
                        cont.resumeWithException(ex)
                    }
                },
                { error ->
                    cont.resumeWithException(error)
                },
                options.cacheMaxTtlSeconds
        )
        enqueueRequest(r, options)
    }

    /** fetch url and return response body as a string */
    suspend fun fetchBodyAsString(url: String, options: RequestOptions = RequestOptions(defaultTimeoutMs)) = suspendCoroutine<String> { cont ->
        maybeInjectRandomError()
        val r = GatewayStringRequest(
            url,
            Request.Priority.NORMAL,
            { response ->
                cont.resumeWith(Result.success(response))
            },
            { error ->
                cont.resumeWithException(error)
            },
            options.cacheMaxTtlSeconds)
        enqueueRequest(r, options)
    }

    /** fetch OPAC url as a browser session with cookies and return response as a string */
    suspend fun fetchOPAC(url: String, authToken: String, options: RequestOptions = RequestOptions(defaultTimeoutMs)) = suspendCoroutine<String> { cont ->
        maybeInjectRandomError()
        val r = OPACRequest(
            url,
            authToken,
            Request.Priority.NORMAL,
            { response ->
                cont.resumeWith(Result.success(response))
            },
            { error ->
                cont.resumeWithException(error)
            },
            options.cacheMaxTtlSeconds)
        enqueueRequest(r, options)
    }

    /** make gateway request and return first payload item as string */
    suspend fun fetchString(service: String, method: String, args: Array<Any?>, shouldCache: Boolean) = fetch(service, method, args, shouldCache) { result ->
        result.payloadFirstAsString()
    }

    // fetchObject - make gateway request and expect json payload of OSRFObject
    suspend fun fetchObject(service: String, method: String, args: Array<Any?>, shouldCache: Boolean) = fetch(service, method, args, shouldCache) { result ->
        result.payloadFirstAsObject()
    }

    // fetchOptionalObject - expect OSRFObject or empty
    suspend fun fetchOptionalObject(service: String, method: String, args: Array<Any?>, shouldCache: Boolean) = fetch(service, method, args, shouldCache) { result ->
        result.payloadFirstAsOptionalObject()
    }

    // fetchObjectArray - make gateway request and expect json payload of [OSRFObject]
    suspend fun     fetchObjectArray(service: String, method: String, args: Array<Any?>, shouldCache: Boolean) = fetch(service, method, args, shouldCache) { result ->
        result.payloadFirstAsObjectList()
    }

    // fetchMaybeEmptyArray - expect json payload of OSRFObjects or empty (not inside extra array)
    suspend fun fetchMaybeEmptyArray(service: String, method: String, args: Array<Any?>, shouldCache: Boolean) = fetch(service, method, args, shouldCache) { result ->
        result.payloadAsObjectList()
    }

    private fun enqueueRequest(r: Request<*>, options: RequestOptions) {
        r.setShouldCache(options.shouldCache)
        r.retryPolicy = DefaultRetryPolicy(
                options.timeoutMs,
                if (options.shouldRetry) 1 else 0,
                0.0f)//do not increase timeout on retry
        Volley.getInstance().addToRequestQueue(r)
    }

    /** for testing, inject an error randomly */
    private fun maybeInjectRandomError() {
        if (randomErrorPercentage <= 0) return
        val r = Random.nextInt(100)
        Log.d(TAG, "[kcxxx] Random error if $r < $randomErrorPercentage")
        if (r < randomErrorPercentage) throw GatewayError("Random error $r < $randomErrorPercentage")
    }
}
