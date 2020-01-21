/*
 * Copyright (c) 2019 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.test

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.evergreen_ils.Api
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.evergreen_ils.utils.getCustomMessage
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

private const val TAG = "GatewayTest"

//@RunWith(AndroidJUnit4::class)
class LiveGatewayTest {

    companion object {

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
            Log.d("hey", "here");

            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            VolleyWrangler.init(ctx)
            Gateway.baseUrl = "https://kenstir.ddns.net"
            Gateway.clientCacheKey = "42"
        }
    }

    @Test
    fun test_basic() {
        assertTrue(true)
        val s = Gateway.baseUrl
        assertNotNull(s)
        val url = Gateway.buildUrl(Api.ACTOR, Api.ILS_VERSION, arrayOf())
        print("url:$url")
        assertNotNull(url)
        Gateway.serverCacheKey = "HEAD"
        val url2 = Gateway.buildUrl(Api.ACTOR, Api.ILS_VERSION, arrayOf())
        print("url:$url2")
        assertNotNull(url2)
    }

    @Test
    fun test_directSuspendFun() {
        runBlocking {
            launch(Dispatchers.IO) {
                val version = Gateway.fetch(Api.ACTOR, Api.ILS_VERSION, arrayOf(), false) { response ->
                    response.asString()
                }
                Log.d(TAG, "version:$version")
                assertTrue(version.isNotEmpty())
            }
        }
    }

    @Test
    fun test_fetchServerVersion() {
        runBlocking {
            launch(Dispatchers.Main) {
                val result = Gateway.actor.fetchServerVersion()
                assertTrue(result is Result.Success)
                if (result is Result.Success) {
                    Log.d(TAG, "version:$result.data")
                    assertTrue(result.data.isNotEmpty())
                }
            }
        }
    }

    // During system maintenance, the server can be up but responding 404 to gateway URLs.
    suspend fun fetchStringNotFound(): Result<String> {
        return try {
            val notFoundUrl = Gateway.baseUrl.plus("/not-found")
            val ret = Gateway.fetchString(notFoundUrl, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @Test
    fun test_clientError404() {
        runBlocking {
            launch(Dispatchers.Main) {
                val result = fetchStringNotFound()
                assertTrue(result is Result.Error)
                if (result is Result.Error) {
                    val ex = result.exception
                    assertEquals("Not found.  The server may be down for maintenance.",
                            ex.getCustomMessage())
                }
            }
        }
    }
}
