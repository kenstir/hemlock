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

package org.evergreen_ils.gateway

import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.kenstir.data.RequestOptions
import net.kenstir.data.HemlockPlugin
import net.kenstir.data.HemlockOkHttpInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
import org.evergreen_ils.Api
import java.io.File
import java.net.URLEncoder

private const val INITIAL_URL_SIZE = 128

/** used to make requests through the OSRF Gateway */
object GatewayClient {
    lateinit var baseUrl: String

    // For notes on caching, see docs/notes-on-caching.md
    lateinit var clientCacheKey: String
    private var _serverCacheKey: String? = null
    private val startTime = System.currentTimeMillis()
    var serverCacheKey: String
        get() = _serverCacheKey ?: startTime.toString()
        set(value) { _serverCacheKey = value }

    private const val GATEWAY_PATH = "/osrf-gateway-v1"
    const val DEFAULT_TIMEOUT_MS = 30_000
    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MiB

    @JvmStatic
    lateinit var cacheDirectory: File
    val client: HttpClient by lazy { makeHttpClient() }
    lateinit var okHttpClient: OkHttpClient
    fun makeHttpClient(): HttpClient {
        val okHttpCache = Cache(cacheDirectory, CACHE_SIZE.toLong())
        okHttpClient = OkHttpClient.Builder()
            .cache(okHttpCache)
            .callTimeout(DEFAULT_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .addInterceptor(HemlockOkHttpInterceptor())
            .build()
        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            install(HemlockPlugin) {}
            install(ContentNegotiation) {
                json(
                    Json { ignoreUnknownKeys = true },
                    contentType = ContentType.Any
                )
            }
        }
    }

    /** Initializes the HttpClient by the lazy initializer */
    @JvmStatic
    fun initHttpClient() {
        client.pluginOrNull(HemlockPlugin)
    }

    /**
     * Encodes a parameter value for use in a URL query string
     *
     * I used to use [android.net.Uri.encode] here, but it is not mocked and so is not convenient
     * to use in unit tests.  But URLEncoder.encode is not quite the same, it replaces spaces with `+`.
     * This is a workaround, and if we find more issues we'll need to revisit this.
     */
    private fun encodeParamValue(v: String): String {
        return URLEncoder.encode(v, "UTF-8").replace("+", "%20")
    }

    fun buildQuery(service: String?, method: String?, params: List<XGatewayParam>, addCacheParams: Boolean = true): String {
        val sb = StringBuilder(INITIAL_URL_SIZE)
        sb.append("service=").append(service)
        sb.append("&method=").append(method)
        for (param in params) {
            sb.append("&param=")
            sb.append(encodeParamValue(Json.encodeToString(param)))
        }

        if (addCacheParams) {
            sb.append("&_ck=").append(clientCacheKey)
            sb.append("&_sk=").append(serverCacheKey)
        }

        return sb.toString()
    }

    fun buildUrl(service: String, method: String, params: List<XGatewayParam>, addCacheParams: Boolean = true): String {
        return baseUrl.plus(GATEWAY_PATH).plus("?").plus(
            buildQuery(service, method, params, addCacheParams)
        )
    }

    fun gatewayUrl(): String {
        return baseUrl.plus(GATEWAY_PATH)
    }

    fun getUrl(relativeUrl: String): String {
        return baseUrl.plus(relativeUrl)
    }

    fun getIDLUrl(shouldCache: Boolean = true): String {
        val params = mutableListOf<String>()
        for (className in Api.IDL_CLASSES_USED.split(",")) {
            params.add("class=$className")
        }
        if (shouldCache) {
            params.add("_ck=$clientCacheKey")
            params.add("_sk=$serverCacheKey")
        }
        return baseUrl.plus("/reports/fm_IDL.xml?")
            .plus(params.joinToString("&"))
    }

    suspend fun fetch(service: String, method: String, params: List<XGatewayParam>, shouldCache: Boolean): GatewayResponse {
        return fetch(service, method, params, RequestOptions(DEFAULT_TIMEOUT_MS, shouldCache, true))
    }

    suspend fun fetch(service: String, method: String, params: List<XGatewayParam>, options: RequestOptions): GatewayResponse {
        if (options.shouldCache) {
            val url = buildUrl(service, method, params, options.shouldCache)
            return GatewayResponse(client.get(url))
        } else {
            val body = buildQuery(service, method, params, options.shouldCache)
            return GatewayResponse(client.post(gatewayUrl()) {
                setBody(body)
                contentType(ContentType.Application.FormUrlEncoded)
            })
        }
    }

    suspend fun get(url: String): GatewayResponse {
        return GatewayResponse(client.get(url))
    }

    suspend fun getOPAC(url: String, authToken: String, shouldCache: Boolean): GatewayResponse {
        return GatewayResponse(client.get(url) {
            header("Cache-Control", if (shouldCache) "max-age=3600" else "no-cache")
            cookie(name = "ses", value = authToken)
            cookie(name = "eg_loggedin", value = "1")
        })
    }
}
