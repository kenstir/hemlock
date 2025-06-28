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
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.util.*
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.debugTagKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.recvTimeKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.sentTimeKey

object HemlockPluginKeys {
    val debugTag = "h.debugTag"
    val recvTime = "h.recvTime"
    val sentTime = "h.sentTime"
}

object HemlockPluginAttributeKeys {
    val debugTagKey = AttributeKey<String>(HemlockPluginKeys.debugTag)
    val recvTimeKey = AttributeKey<Long>(HemlockPluginKeys.recvTime)
    val sentTimeKey = AttributeKey<Long>(HemlockPluginKeys.sentTime)
}

/**
 * a ktor client plugin that tracks response times and detects responses cached by the HttpCache plugin.
 */
val HemlockPlugin = createClientPlugin("HemlockPlugin") {

    // We calculate debugTag here, because it's easier.  For POST requests, [content] is a String;
    // for GET requests, [content] as? String is null.  By the time we get to the SendingRequest hook,
    // [content] is OutputContent.
    onRequest { requestBuilder, content ->
        val tag = debugTag(requestBuilder.url.toString(), content as? String)
        requestBuilder.attributes.put(debugTagKey, tag)
        println("[net] %8s onRequest:      %s, $content".format(tag, requestBuilder.url))
//        val now = System.currentTimeMillis()
//        println("%.3f: onRequest:      %s".format(now.toDouble() / 1000, requestBuilder.url))
    }

    on(SendingRequest) { request, content ->
        val now = System.currentTimeMillis()
        request.attributes.put(sentTimeKey, now)
        val tag = request.attributes[debugTagKey]
        println("[net] %8s SendingRequest: %s, $content".format(tag, request.url))

//        println("%.3f: SendingRequest: %s with content length ${content.contentLength ?: "unknown"} bytes".format(now.toDouble() / 1000, request.url))
    }

    onResponse { response ->
        val now = System.currentTimeMillis()
        response.call.request.attributes.put(recvTimeKey, now)
        val sent = response.call.request.attributes[sentTimeKey]
        val elapsed = now - sent
        val tag = response.call.request.attributes[debugTagKey]
        val url = response.call.request.url
        println("[net] %8s onResponse:     %s".format(tag, url))
//        println("%.3f: onResponse:     %s with status ${response.status.value} in ${elapsed}ms".format(now.toDouble() / 1000, response.call.request.url))
    }
}

/**
 * return true if the response was cached by the HttpCache plugin.
 *
 * We infer that a response was cached if the `onResponse` hook was not called
 */
//fun isCached(response: HttpResponse): Boolean {
//    return !response.call.attributes.contains(recvTimeKey)
//}
fun HttpResponse.isCached(): Boolean {
    return !this.call.attributes.contains(recvTimeKey)
}

/**
 * return the elapsed time in milliseconds between send and receive, 0 if the response was cached
 */
//fun elapsedTime(response: HttpResponse): Long {
//    if (!response.call.attributes.contains(recvTimeKey)) {
//        return 0
//    }
//    val recvTime = response.call.attributes[recvTimeKey]
//    val sentTime = response.call.attributes[sentTimeKey]
//    return recvTime - sentTime
//}
fun HttpResponse.elapsedTime(): Long {
    if (!this.call.attributes.contains(recvTimeKey)) {
        return 0
    }
    val recvTime = this.call.attributes[recvTimeKey]
    val sentTime = this.call.attributes[sentTimeKey]
    return recvTime - sentTime
}

fun debugTag(url: String, content: String?): String {
    return if (content == null) {
        Integer.toHexString(url.hashCode())
    } else {
        Integer.toHexString("${url}?$content".hashCode())
    }
}
