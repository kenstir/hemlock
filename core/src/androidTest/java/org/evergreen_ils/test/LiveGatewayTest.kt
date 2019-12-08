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
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
                val version: String = Gateway.actor.fetchServerVersion()
                Log.d(TAG, "version:$version")
                assertTrue(version.isNotEmpty())
            }
        }
    }
}
