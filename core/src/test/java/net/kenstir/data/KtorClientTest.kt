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

package net.kenstir.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import net.kenstir.util.Analytics
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.paramListOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

// Tests for Ktor; client caching and HemlockPlugin functionality
class KtorClientTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val testServer = getRequiredProperty("testEvergreenServer")

            GatewayClient.baseUrl = testServer
            GatewayClient.clientCacheKey = System.currentTimeMillis().toString()
            GatewayClient.cacheDirectory = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "KtorClientTest")
            GatewayClient.cacheDirectory.deleteRecursively()

            client = GatewayClient.makeHttpClient()
        }

        fun getRequiredProperty(name: String): String {
            return System.getProperty(name) ?: throw RuntimeException("Missing required system property: $name")
        }

        lateinit var client: HttpClient
    }

    @Test
    fun test_pluginIsInstalled() {
        assertNotNull("HemlockPlugin should be installed", client.pluginOrNull(HemlockPlugin))
    }

    @Test
    fun test_get_withCaching() = runTest {
        val url = GatewayClient.buildUrl(Api.ACTOR, Api.ORG_UNIT_RETRIEVE, paramListOf(Api.ANONYMOUS, 1))

        run {
            val response = client.get(url)

            // call bodyAsText to ensure the response is fully received before checking cache
            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertFalse("First response should not be cached", isCached)
            assertTrue("Non-cached response should take non-zero time", elapsed > 0)
        }

        // OkHttp cache needs no sleep
        //Thread.sleep(100)

        run {
            val response = client.get(url)

            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertTrue("Second response should be cached", isCached)
        }
    }

    @Test
    fun test_get_withCachingDisabled() = runTest {
        val url = GatewayClient.buildUrl(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        run {
            val response = client.get(url) {
                headers.append(HttpHeaders.CacheControl, CacheControl.NO_STORE)
            }

            // call bodyAsText to ensure the response is fully received before checking cache
            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertFalse("First response should not be cached", isCached)
        }

        // OkHttp cache needs no sleep
        //Thread.sleep(100)

        run {
            val response = client.get(url) {
                headers.append(HttpHeaders.CacheControl, CacheControl.NO_STORE)
            }

            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertFalse("Second response should not be cached", isCached)
        }
    }

    // Verify that POST requests are not cached
    @Test
    fun test_post_withParams() = runTest {
        val url = GatewayClient.gatewayUrl()
        val requestBody = GatewayClient.buildQuery(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        run {
            val response = client.post(url) {
                setBody(requestBody)
                contentType(ContentType.Application.FormUrlEncoded)
            }

            // call bodyAsText to ensure the response is fully received before checking cache
            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertFalse("First response should not be cached", isCached)
        }

        // OkHttp cache needs no sleep
        //Thread.sleep(100)

        run {
            val response = client.post(url) {
                setBody(requestBody)
                contentType(ContentType.Application.FormUrlEncoded)
            }

            val body = response.bodyAsText()
            val elapsed = response.elapsedTime()
            val isCached = response.isCached()
            Analytics.logResponseX(response.debugTag(), response.debugUrl(), isCached, body, elapsed)
            assertFalse("Second response should not be cached", isCached)
        }
    }
}
