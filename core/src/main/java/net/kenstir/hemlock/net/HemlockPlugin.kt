/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.hemlock.net

import io.ktor.client.plugins.api.*
import io.ktor.client.statement.HttpResponse
import io.ktor.util.*
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.debugTagKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.debugUrlKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.fromCacheKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.recvTimeKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.sentTimeKey
import okhttp3.Interceptor

object HemlockPluginKeys {
    val debugTag = "h.debugTag"
    val debugUrl = "h.debugUrl"
    val fromCache = "h.fromCache"
    val recvTime = "h.recvTime"
    val sentTime = "h.sentTime"
}

object HemlockPluginAttributeKeys {
    /** hash value of the request, for logging */
    val debugTagKey = AttributeKey<String>(HemlockPluginKeys.debugTag)
    /** URL of the request, for logging, with POST body expressed as parameters */
    val debugUrlKey = AttributeKey<String>(HemlockPluginKeys.debugUrl)
    /** true if the response was cached by the OkHttp engine */
    val fromCacheKey = AttributeKey<Boolean>(HemlockPluginKeys.fromCache)
    /** time the response was received */
    val recvTimeKey = AttributeKey<Long>(HemlockPluginKeys.recvTime)
    /** time the request was sent */
    val sentTimeKey = AttributeKey<Long>(HemlockPluginKeys.sentTime)
}

private const val X_FROM_CACHE = "X-From-Cache"

/**
 * a ktor client plugin that tracks response times and detects responses cached by the HttpCache plugin.
 */
val HemlockPlugin = createClientPlugin("HemlockPlugin") {

    // We calculate debugUrl and debugTag here, because it's easier.
    // For POST requests, [content] is a String; for GET requests, `content as? String` is null.
    // By the time we get to the SendingRequest hook, [content] is OutputContent.
    onRequest { requestBuilder, content ->
        // construct a URL for logging that looks like path?query, even for POST requests
        var debugUrl = requestBuilder.url.toString()
        (content as? String)?.let { debugUrl += "?$it" }

        val debugTag = Integer.toHexString(debugUrl.hashCode())

        requestBuilder.attributes.put(debugTagKey, debugTag)
        requestBuilder.attributes.put(debugUrlKey, debugUrl)
    }

    on(SendingRequest) { request, _ ->
        val now = System.currentTimeMillis()
        request.attributes.put(sentTimeKey, now)
        val debugTag = request.attributes[debugTagKey]
        val debugUrl = request.attributes[debugUrlKey]
        Analytics.logRequest(debugTag, request.method.toString(), debugUrl)
    }

    onResponse { response ->
        val now = System.currentTimeMillis()
        response.call.request.attributes.put(recvTimeKey, now)

        val fromCacheHeader = response.headers[X_FROM_CACHE]
        val fromCache = fromCacheHeader != null
        response.call.request.attributes.put(fromCacheKey, fromCache)
    }
}

/**
 * An OkHttp interceptor that adds a header to cached responses
 * so we can detect them in HemlockPlugin.
 */
class HemlockOkHttpInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val response = chain.proceed(chain.request())

        // response.cacheResponse is non-null if the response exists in the cache.
        // But that does not mean it will be served from the cache.  In the absence
        // of caching headers, OkHttp will send a request with If-Modified-Since,
        // and that response is in response.networkResponse.  We only consider the
        // response cached if we completely avoided the network.
        val servedEntirelyFromCache = response.cacheResponse != null && response.networkResponse == null
        if (!servedEntirelyFromCache) {
            // If the response is not cached, we don't modify it.
            return response
        }
        val modifiedResponse = response.newBuilder()
            .addHeader(X_FROM_CACHE, "1")
            .build()
        return modifiedResponse
    }
}

/**
 * return true if the response was cached
 */
fun HttpResponse.isCached(): Boolean {
    // With the HttpCache plugin, we had to infer that the response was cached by noting that the
    // onResponse hook was not called (i.e., recvTimeKey was not set).
    //return !this.call.attributes.contains(recvTimeKey) // HttpCache plugin approach

    // With the OkHttp engine, we rely on HemlockOkHttpInterceptor and the fromCacheKey attribute.
    return this.call.request.attributes[fromCacheKey]
}

/**
 * return the elapsed time in milliseconds between send and receive, 0 if the response was cached
 */
fun HttpResponse.elapsedTime(): Long {
    val recvTime = this.call.attributes.getOrNull(recvTimeKey)
        ?: return 0
    val sentTime = this.call.attributes[sentTimeKey]
    return recvTime - sentTime
}

/**
 * return the debug tag aka requestId for the request, used to match requests and responses in the log
 */
fun HttpResponse.debugTag(): String {
    return this.call.request.attributes[debugTagKey]
}

fun HttpResponse.debugUrl(): String {
    return this.call.request.attributes[debugUrlKey]
}
