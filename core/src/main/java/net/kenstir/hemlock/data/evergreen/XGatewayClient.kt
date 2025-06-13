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

package net.kenstir.hemlock.data.evergreen

import android.net.Uri
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.kenstir.hemlock.data.RequestOptions

private const val INITIAL_URL_SIZE = 128

/** used to make requests through the OSRF Gateway */
object XGatewayClient {
    lateinit var baseUrl: String

    // For notes on caching, see docs/notes-on-caching.md
    lateinit var clientCacheKey: String
    private var _serverCacheKey: String? = null
    private val startTime = System.currentTimeMillis()
    var serverCacheKey: String
        get() = _serverCacheKey ?: startTime.toString()
        set(value) { _serverCacheKey = value }

    private const val GATEWAY_PATH = "/osrf-gateway-v1"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true },
                contentType = ContentType.Any
            )
        }
    }

    fun buildQuery(service: String?, method: String?, params: Array<Any?>, addCacheArgs: Boolean = true): String {
        val sb = StringBuilder(INITIAL_URL_SIZE)
        sb.append("service=").append(service)
        sb.append("&method=").append(method)
        for (param in params) {
            sb.append("&param=")
            sb.append(Uri.encode(Json.encodeToString(param), "UTF-8"))
        }

        if (addCacheArgs) {
            sb.append("&_ck=").append(clientCacheKey)
            sb.append("&_sk=").append(serverCacheKey)
        }

        return sb.toString()
    }

    fun buildUrl(service: String, method: String, args: Array<Any?>, addCacheArgs: Boolean = true): String {
        return baseUrl.plus("/osrf-gateway-v1?").plus(
            buildQuery(service, method, args, addCacheArgs)
        )
    }

    fun getUrl(relativeUrl: String): String {
        return baseUrl.plus(relativeUrl)
    }

    suspend fun fetch(service: String, method: String, args: Array<Any?>, shouldCache: Boolean): XGatewayResponse {
        return fetch(service, method, args, RequestOptions(30_000, shouldCache, true))
    }

    suspend fun fetch(service: String, method: String, args: Array<Any?>, options: RequestOptions): XGatewayResponse {
        val url = buildUrl(service, method, args, options.shouldCache)
        return client.get(url).body()
    }
}
