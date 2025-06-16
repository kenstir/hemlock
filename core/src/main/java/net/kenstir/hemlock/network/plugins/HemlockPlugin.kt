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

package net.kenstir.hemlock.network.plugins

import io.ktor.client.plugins.api.*
import io.ktor.client.statement.HttpResponse
import io.ktor.util.*
import net.kenstir.hemlock.network.plugins.HemlockPluginAttributeKeys.sentTimeKey

object HemlockPluginKeys {
    val onResponse = "h.onResponse"
    val sentTime = "h.sentTime"
}

object HemlockPluginAttributeKeys {
    val onResponseKey = AttributeKey<Boolean>(HemlockPluginKeys.onResponse)
    val sentTimeKey = AttributeKey<Long>(HemlockPluginKeys.sentTime)
}

val HemlockPlugin = createClientPlugin("HemlockPlugin") {

    onRequest { request, _ ->
        val now = System.currentTimeMillis()
        println("%.3f: onRequest: ${request.url}".format(now.toDouble() / 1000))
    }

    on(SendingRequest) { request, content ->
        val now = System.currentTimeMillis()
        request.attributes.put(sentTimeKey, now)
        println("%.3f: send: ${request.url} with content length ${content.contentLength ?: "unknown"} bytes".format(now.toDouble() / 1000))
    }

    onResponse { response ->
        response.call.attributes.put(HemlockPluginAttributeKeys.onResponseKey, true)
        val sent = response.call.attributes[sentTimeKey]
        val now = System.currentTimeMillis()
        val elapsed = now - sent
        println("%.3f: recv: ${response.call.request.url} with status ${response.status.value} in ${elapsed}ms".format(now.toDouble() / 1000))
    }
}

// extension function to check if the response was cached
// We infer that a response was cached if the `onResponse` hook was not called
fun isCached(response: HttpResponse): Boolean {
    return !response.call.attributes.contains(HemlockPluginAttributeKeys.onResponseKey)
}
