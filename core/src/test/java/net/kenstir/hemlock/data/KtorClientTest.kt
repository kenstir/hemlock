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

package net.kenstir.hemlock.data

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
import org.evergreen_ils.Api
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.paramListOf
import net.kenstir.hemlock.net.HemlockPlugin
import net.kenstir.hemlock.net.elapsedTime
import net.kenstir.hemlock.net.isCached
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

            XGatewayClient.baseUrl = testServer
            XGatewayClient.clientCacheKey = System.currentTimeMillis().toString()
            XGatewayClient.cacheDirectory = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "KtorClientTest")
            XGatewayClient.cacheDirectory.deleteRecursively()

            client = XGatewayClient.makeHttpClient()
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
        val url = XGatewayClient.buildUrl(Api.ACTOR, Api.ORG_UNIT_RETRIEVE, paramListOf(Api.ANONYMOUS, 1))

        val response1 = client.get(url)

        // call bodyAsText to ensure the response is fully received before checking cache
        val responseBody1 = response1.bodyAsText()
        val elapsed1 = response1.elapsedTime()
        val cached1 = response1.isCached()
        println("try1: ${elapsed1}ms: $responseBody1")
        assertFalse("First response should not be cached", cached1)
        assertTrue("Non-cached response should take non-zero time", elapsed1 > 0)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(100)

        val response2 = client.get(url)

        val responseBody2 = response2.bodyAsText()
        val elapsed2 = response2.elapsedTime()
        val cached2 = response2.isCached()
        println("try2: ${elapsed2}ms: $responseBody2")
        assertTrue("Second response should be cached", cached2)
        //assertEquals(0, elapsed2)
    }

    @Test
    fun test_get_withCachingDisabled() = runTest {
        val url = XGatewayClient.buildUrl(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        val response1 = client.get(url) {
            headers.append(HttpHeaders.CacheControl, CacheControl.NO_STORE)
        }

        // call bodyAsText to ensure the response is fully received before checking cache
        val responseBody1 = response1.bodyAsText()
        val elapsed1 = response1.elapsedTime()
        val cached1 = response1.isCached()
        println("try1: ${elapsed1}ms: $responseBody1")
        assertFalse("First response should not be cached", cached1)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(100)

        val response2 = client.get(url) {
            headers.append(HttpHeaders.CacheControl, CacheControl.NO_STORE)
        }

        val responseBody2 = response2.bodyAsText()
        val elapsed2 = response2.elapsedTime()
        val cached2 = response2.isCached()
        println("try2: ${elapsed2}ms: $responseBody2")
        assertFalse("Second response should not be cached", cached2)
    }

    // Verify that POST requests are not cached
    @Test
    fun test_post_withParams() = runTest {
        val url = XGatewayClient.gatewayUrl()
        val body = XGatewayClient.buildQuery(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        val response1 = client.post(url) {
            setBody(body)
            contentType(ContentType.Application.FormUrlEncoded)
        }

        // call bodyAsText to ensure the response is fully received before checking cache
        val responseBody1 = response1.bodyAsText()
        val elapsed1 = response1.elapsedTime()
        val cached1 = response1.isCached()
        println("try1: ${elapsed1}ms: $responseBody1")
        assertFalse("First response should not be cached", cached1)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(100)

        val response2 = client.post(url) {
            setBody(body)
            contentType(ContentType.Application.FormUrlEncoded)
        }

        val responseBody2 = response2.bodyAsText()
        val elapsed2 = response2.elapsedTime()
        val cached2 = response2.isCached()
        println("try2: ${elapsed2}ms: $responseBody2")
        assertFalse("Second response should not be cached", cached2)
    }
}
