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
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.recvTimeKey
import net.kenstir.hemlock.net.HemlockPluginAttributeKeys.sentTimeKey

object HemlockPluginKeys {
    //val onResponse = "h.onResponse"
    val recvTime = "h.recvTime"
    val sentTime = "h.sentTime"
}

object HemlockPluginAttributeKeys {
    //val onResponseKey = AttributeKey<Boolean>(HemlockPluginKeys.onResponse)
    val recvTimeKey = AttributeKey<Long>(HemlockPluginKeys.recvTime)
    val sentTimeKey = AttributeKey<Long>(HemlockPluginKeys.sentTime)
}

/**
 * a ktor client plugin that tracks response times and detects responses cached by the HttpCache plugin.
 */
val HemlockPlugin = createClientPlugin("HemlockPlugin") {

    onRequest { request, _ ->
        val now = System.currentTimeMillis()
        println("%.3f: onRequest: %s".format(now.toDouble() / 1000, request.url))
    }

    on(SendingRequest) { request, content ->
        val now = System.currentTimeMillis()
        request.attributes.put(sentTimeKey, now)
        println("%.3f: send: %s with content length ${content.contentLength ?: "unknown"} bytes".format(now.toDouble() / 1000, request.url))
    }

    onResponse { response ->
        val now = System.currentTimeMillis()
        response.call.attributes.put(recvTimeKey, now)
        val sent = response.call.attributes[sentTimeKey]
        val elapsed = now - sent
        println("%.3f: recv: %s with status ${response.status.value} in ${elapsed}ms".format(now.toDouble() / 1000, response.call.request.url))
    }
}

/**
 * returns true if the response was cached by the HttpCache plugin.
 *
 * We infer that a response was cached if the `onResponse` hook was not called
 */
fun isCached(response: HttpResponse): Boolean {
    return !response.call.attributes.contains(recvTimeKey)
}

/**
 * returns the elapsed time in milliseconds between send and receive, 0 if the response was cached
 */
fun elapsedTime(response: HttpResponse): Long {
    if (!response.call.attributes.contains(recvTimeKey)) {
        return 0
    }
    val recvTime = response.call.attributes[recvTimeKey]
    val sentTime = response.call.attributes[sentTimeKey]
    return recvTime - sentTime
}
