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
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.evergreen.XGatewayClient
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
            install(HttpCache)
        }
    }

    @Test
    fun test_fetchServerVersion() = runTest {
        val url = XGatewayClient.buildUrl("open-ils.actor", "opensrf.open-ils.system.ils_version", arrayOf())

        val response1 = cachingClient.get(url)
        println("try1: $response1")
        println("Status: ${response1.status}")
        println("Headers: ${response1.headers.entries()}")
        println("Body length: ${response1.bodyAsText().length}")

        // sleep for a short time to allow cache to be populated
        Thread.sleep(5000)

        val response2 = cachingClient.get(url)
        println("try2: $response2")
        println("Status: ${response2.status}")
        println("Headers: ${response2.headers.entries()}")
        println("Body length: ${response2.bodyAsText().length}")
    }
}
