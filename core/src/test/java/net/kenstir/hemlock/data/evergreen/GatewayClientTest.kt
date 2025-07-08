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

import io.ktor.client.plugins.pluginOrNull
import kotlinx.coroutines.test.runTest
import net.kenstir.data.HemlockPlugin
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.paramListOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class GatewayClientTest {
    val client = GatewayClient.client

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val testServer = getRequiredProperty("testEvergreenServer")
            val testUsername = getRequiredProperty("testEvergreenUsername")
            val testPassword = getRequiredProperty("testEvergreenPassword")

            GatewayClient.baseUrl = testServer
            GatewayClient.clientCacheKey = System.currentTimeMillis().toString()
            GatewayClient.cacheDirectory = File(System.getProperty("java.io.tmpdir") ?: "/tmp", "KtorClientTest")
            GatewayClient.cacheDirectory.deleteRecursively()
        }

        fun getRequiredProperty(name: String): String {
            return System.getProperty(name) ?: throw RuntimeException("Missing required system property: $name")
        }
    }

    @Test
    fun test_pluginIsInstalled() {
        assertNotNull("HemlockPlugin should be installed", client.pluginOrNull(HemlockPlugin))
    }

    // TODO: Run this test with a mock server.  This test will fail if the server does not send cache headers.
    @Test
    fun test_fetch_withCaching() = runTest {
        val response1 = GatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_RETRIEVE, paramListOf(Api.ANONYMOUS, 1), true)

        // call bodyAsText to ensure the response is fully received before checking cache
        val body1 = response1.bodyAsText()
        val elapsed1 = response1.elapsed
        val cached1 = response1.isCached
        println("try1: ${elapsed1}ms: $body1")
        assertFalse("First response should not be cached", cached1)
        assertTrue("Non-cached response should take non-zero time", elapsed1 > 0)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(100)

        val response2 = GatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_RETRIEVE, paramListOf(Api.ANONYMOUS, 1), true)

        val body2 = response2.bodyAsText()
        val elapsed2 = response2.elapsed
        val cached2 = response2.isCached
        println("try2: ${elapsed2}ms: $body2")
        assertTrue("Second response should be cached", cached2)
    }

    @Test
    fun test_fetch_withCachingDisabled() = runTest {
        val response1 = GatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        // call bodyAsText to ensure the response is fully received before checking cache
        val body1 = response1.bodyAsText()
        val elapsed1 = response1.elapsed
        val cached1 = response1.isCached
        println("try1: ${elapsed1}ms: $body1")
        assertFalse("First response should not be cached", cached1)
        assertTrue("Non-cached response should take non-zero time", elapsed1 > 0)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(100)

        val response2 = GatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)

        val body2 = response2.bodyAsText()
        val elapsed2 = response2.elapsed
        val cached2 = response2.isCached
        println("try2: ${elapsed2}ms: $body2")
        assertFalse("Second response should not be cached", cached2)
        assertTrue("Non-cached response should take non-zero time", elapsed2 > 0)
    }
}
