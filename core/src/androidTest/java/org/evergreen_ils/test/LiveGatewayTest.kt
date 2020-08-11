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

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.TimeoutError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.evergreen_ils.Api
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.Volley
import org.evergreen_ils.utils.getCustomMessage
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class LiveGatewayTest {

    companion object {
        private val TAG = LiveGatewayTest::class.java.simpleName

        lateinit var args: Bundle

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            Volley.init(ctx)

            // See root build.gradle for notes on customizing instrumented test variables
            args = InstrumentationRegistry.getArguments()
            val server = args.getString("server")
            val username = args.getString("username")
            val password = args.getString("password")

            Gateway.baseUrl = server
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

    suspend fun fetchStringWithDelay(timeoutMs: Int, delaySeconds: Float): Result<String> {
        val oldTimeoutMs = Gateway.defaultTimeoutMs
        Gateway.defaultTimeoutMs = timeoutMs
        return try {
            val url = args.getString("httpbinServer").plus("/delay/$delaySeconds")
            val ret = Gateway.fetchString(url, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        } finally {
            Gateway.defaultTimeoutMs = oldTimeoutMs
        }
    }

    private fun logResult(result: Result<*>) {
        when (result) {
            is Result.Success -> Log.d(TAG, "success: ${result.data}")
            is Result.Error -> Log.d(TAG, "error: ${result.exception}")
        }
    }

    // With 0.5s delay, 100ms timeout, and 1 retry, this should fail with a TimeoutError
    @Test
    fun test_failWithTimeoutError() {
        runBlocking {
            launch(Dispatchers.Main) {
                val result = fetchStringWithDelay(100, 0.5f)
                logResult(result)
                assertFalse(result.succeeded)
                assertTrue(result.unwrappedError is TimeoutError)
            }
        }
    }

    // With 0 delay, 10s timeout, this should succeed
    @Test
    fun test_completesWithinTimeout() {
        runBlocking {
            launch(Dispatchers.Main) {
                val result = fetchStringWithDelay(10_000, 0f)
                logResult(result)
                assertTrue(result.succeeded)
            }
        }
    }
}
