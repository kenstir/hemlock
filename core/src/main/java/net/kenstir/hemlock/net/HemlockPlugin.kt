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
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.recvTimeKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.sentTimeKey

object HemlockPluginKeys {
    val debugTag = "h.debugTag"
    val debugUrl = "h.debugUrl"
    val recvTime = "h.recvTime"
    val sentTime = "h.sentTime"
}

object HemlockPluginAttributeKeys {
    /** hash value of the request, for logging */
    val debugTagKey = AttributeKey<String>(HemlockPluginKeys.debugTag)
    /** URL of the request, for logging, with POST body expressed as parameters */
    val debugUrlKey = AttributeKey<String>(HemlockPluginKeys.debugUrl)
    /** time the response was received */
    val recvTimeKey = AttributeKey<Long>(HemlockPluginKeys.recvTime)
    /** time the request was sent */
    val sentTimeKey = AttributeKey<Long>(HemlockPluginKeys.sentTime)
}

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
        (content as? String)?.let { debugUrl += "?$it" } // append ?queryString for POST requests

        val debugTag = Integer.toHexString(debugUrl.hashCode())

        requestBuilder.attributes.put(debugTagKey, debugTag)
        requestBuilder.attributes.put(debugUrlKey, debugUrl)
    }

    on(SendingRequest) { request, _ ->
        val now = System.currentTimeMillis()
        request.attributes.put(sentTimeKey, now)
        val debugTag = request.attributes[debugTagKey]
        val debugUrl = request.attributes[debugUrlKey]
//        println("[net] %8s SendingRequest: %s".format(debugTag, debugUrl))
        Analytics.logRequest(debugTag, debugUrl)
    }

    onResponse { response ->
        val now = System.currentTimeMillis()
        response.call.request.attributes.put(recvTimeKey, now)
//        val tag = response.call.request.attributes[debugTagKey]
//        val debugUrl = response.call.request.attributes[debugUrlKey]
//        println("%8s onResponse:     %s".format(tag, debugUrl))
    }
}

/**
 * return true if the response was cached by the HttpCache plugin.
 *
 * We infer that a response was cached if the `onResponse` hook was not called
 */
fun HttpResponse.isCached(): Boolean {
    return !this.call.attributes.contains(recvTimeKey)
}

/**
 * return the elapsed time in milliseconds between send and receive, 0 if the response was cached
 */
fun HttpResponse.elapsedTime(): Long {
    if (!this.call.attributes.contains(recvTimeKey)) {
        return 0
    }
    val recvTime = this.call.attributes[recvTimeKey]
    val sentTime = this.call.attributes[sentTimeKey]
    return recvTime - sentTime
}

fun HttpResponse.debugTag(): String {
    return this.call.request.attributes[debugTagKey]
}

fun HttpResponse.debugUrl(): String {
    return this.call.request.attributes[debugUrlKey]
}
