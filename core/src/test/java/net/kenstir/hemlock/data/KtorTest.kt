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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.evergreen.XGatewayClient
import net.kenstir.hemlock.data.evergreen.paramListOf
import net.kenstir.hemlock.network.plugins.HemlockPlugin
import net.kenstir.hemlock.network.plugins.HemlockPluginAttributeKeys
import net.kenstir.hemlock.network.plugins.isCached
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class KtorTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val testServer = getRequiredProperty("testEvergreenServer")
            val testUsername = getRequiredProperty("testEvergreenUsername")
            val testPassword = getRequiredProperty("testEvergreenPassword")

            XGatewayClient.baseUrl = testServer
            XGatewayClient.clientCacheKey = System.currentTimeMillis().toString()
        }

        fun getRequiredProperty(name: String): String {
            return System.getProperty(name) ?: throw RuntimeException("Missing required system property: $name")
        }

        val cachingClient = HttpClient(CIO) {
            install(HttpCache) {
                println("plugin: HttpCache using in-memory cache")
            }
            install(Logging) {
                println("plugin: Logging")
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
            install(HemlockPlugin) {
                println("plugin: HemlockPlugin")
            }
        }
    }

    @Test
    fun test_fetchServerVersion() = runTest {
        val url = XGatewayClient.buildUrl("open-ils.actor", "opensrf.open-ils.system.ils_version", paramListOf())

        val response1 = cachingClient.get(url)
        println("try1: $response1")
        val sent1 = response1.call.attributes[HemlockPluginAttributeKeys.sentTimeKey]
        val cached1 = isCached(response1)
        assertFalse("Response should not be cached on first request", cached1)

        // sleep for a short time to allow cache to be populated
        Thread.sleep(500)

        val response2 = cachingClient.get(url)
        println("try2: $response2")
        val cached2 = isCached(response2)
        assertTrue("Response should be cached on second request", cached2)
    }
}
