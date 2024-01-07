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
import kotlinx.coroutines.*
import org.evergreen_ils.Api
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.auth.EvergreenAuthenticator
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.*
import org.evergreen_ils.system.EgIDL
import org.evergreen_ils.utils.getCustomMessage
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject
import org.opensrf.util.OSRFRegistry

class LiveGatewayTest {

    companion object {
        private val TAG = LiveGatewayTest::class.java.simpleName

        lateinit var args: Bundle
        lateinit var server: String
        lateinit var username: String
        lateinit var password: String
        lateinit var account: Account

        var authToken = ""
        //var bookbagId: Int? = 1197349 // cool list with 1 deleted item
        //var bookbagId: Int? = 1194240 // cool list with 1 item
        var bookbagId: Int? = null

        var isIDLLoaded = false

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            Volley.init(ctx)

            // See root build.gradle for notes on customizing instrumented test variables (hint: secret.gradle)
            args = InstrumentationRegistry.getArguments()
            server = args.getString("server") ?: "https://demo.evergreencatalog.com/"
            username = args.getString("username") ?: "no-such-user"
            password = args.getString("password") ?: "password1"

            account = Account(username)

            Gateway.baseUrl = server
            Gateway.clientCacheKey = "42"
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            if (authToken.isEmpty()) return

            Log.d(TAG, "deleting session with authToken=${authToken}")
            runBlocking {
                launch(Dispatchers.Main) {
                    Gateway.auth.deleteSession(authToken)
                }
            }
        }

        // stripped down version of LaunchActivity.getAccount()
        @JvmStatic
        fun getAccount() {
            if (authToken.isNotEmpty()) return

            authToken = EvergreenAuthenticator.signIn(server, username, password)
            Log.d(TAG, "authToken=${authToken}")
            account.authToken = authToken
            App.setAccount(account)
        }

        // stripped down version of LaunchActivity.getSession()
        @JvmStatic
        suspend fun getSession() {
            if (account.id != null) return

            val sessionResult = Gateway.auth.fetchSession(authToken)
            when (sessionResult) {
                is Result.Success -> account.loadSession(sessionResult.data)
                is Result.Error -> {
                    throw sessionResult.exception
                }
            }
        }

        // stripped down version of LaunchViewModel.loadServiceData()
        @JvmStatic
        suspend fun loadServiceData() {
            if (!isIDLLoaded) {
                Log.d(TAG, "registry:${OSRFRegistry.getRegistry("au")}")
                EgIDL.loadIDL()
                Log.d(TAG, "registry:${OSRFRegistry.getRegistry("au")}")
                isIDLLoaded = true
            }
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
            launch(Dispatchers.Main) {
                val version = Gateway.fetch(Api.ACTOR, Api.ILS_VERSION, arrayOf(), false) { response ->
                    response.payloadFirstAsString()
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
            val ret = Gateway.fetchString(notFoundUrl)
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
        return try {
            val url = args.getString("httpbinServer").plus("/delay/$delaySeconds")
            val ret = Gateway.fetchString(url, RequestOptions(timeoutMs))
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
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

    @Test
    fun test_loadBookbags() {
        getAccount()
        runBlocking {
            launch(Dispatchers.Main) {
                loadServiceData()
                getSession()

                val jobs = mutableListOf<Job>()

                // fetch bookbags
                when (val result = GatewayLoader.loadBookBagsAsync(App.getAccount())) {
                    is Result.Success -> {}
                    is Result.Error -> { throw result.exception }
                }

                // flesh bookbags
                for (bookBag in App.getAccount().bookBags) {
                    jobs.add(async {
                        GatewayLoader.loadBookBagContents(App.getAccount(), bookBag, true)
                    })
                }

                jobs.joinAll()
                assertTrue(true)
            }
        }
    }

    @Test
    fun test_loadBookbagContents() {
        val id = bookbagId ?: return

        getAccount()
        runBlocking {
            launch(Dispatchers.Main) {
                loadServiceData()
                getSession()

                val bookBag = BookBag(id, "a list", OSRFObject())
                val result = Gateway.actor.fleshBookBagAsync(account, id)
                logResult(result)
                val obj = result.get()
                bookBag.fleshFromObject(obj)

                assertTrue(true)
            }
        }
    }

    @Test
    fun test_loadBookbagContentsViaQuery() {
        val id = bookbagId ?: return

        getAccount()
        runBlocking {
            launch(Dispatchers.Main) {
                loadServiceData()
                getSession()

                val query = "container(bre,bookbag,${id},${authToken})"
                val result = Gateway.search.fetchMulticlassQuery(query, 1)
                when (result) {
                    is Result.Success -> Log.d(TAG, "bag success: ${result.data}")
                    is Result.Error -> Log.d(TAG, "bag error: ${result.exception}")
                }
                val stuff = result.get()
                Log.d(TAG, "stuff=${stuff}")
                val bookBag = BookBag(id, "a list", OSRFObject())
                bookBag.initVisibleIdsFromQuery(stuff)
            }
        }
    }

    @Test
    fun test_updateMessageViaOPACRequest() {
        val messageId = 28295855
        val action = "mark_unread"
        val url = Gateway.baseUrl.plus("/eg/opac/myopac/messages?action=$action&message_id=$messageId");

        getAccount()
        runBlocking {
            launch(Dispatchers.Main) {
                loadServiceData()
                getSession()

                val s = Gateway.fetchOPAC(url, authToken)
//                Log.d(TAG, "s=$s")
            }
        }
    }
}
