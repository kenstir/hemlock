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
import net.kenstir.hemlock.data.RequestOptions
import net.kenstir.hemlock.data.evergreen.Api
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.android.StdoutLogProvider
import net.kenstir.hemlock.android.accounts.EvergreenAuthenticator
import net.kenstir.hemlock.data.models.Account
import org.evergreen_ils.data.BookBag
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.net.*
import org.evergreen_ils.utils.getCustomMessage
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject
import org.opensrf.util.OSRFRegistry

class LiveGatewayTest {

    /*

    // During system maintenance, the server can be up but responding 404 to gateway URLs.
    suspend fun fetchStringNotFound(): Result<String> {
        return try {
            val notFoundUrl = Gateway.baseUrl.plus("/not-found")
            val ret = Gateway.fetchBodyAsString(notFoundUrl)
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

    private suspend fun fetchStringWithDelay(timeoutMs: Int, delaySeconds: Float): Result<String> {
        return try {
            val url = args.getString("httpbinServer").plus("/delay/$delaySeconds")
            val ret = Gateway.fetchBodyAsString(url, RequestOptions(timeoutMs))
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

    // One-off test should not run because it updates the account
    // and requires creating the User Setting Type.
    //@Test
    fun test_storeUserData() {
        getAccount()
        runBlocking {
            launch(Dispatchers.Main) {
                loadServiceData()
                getSession()

                val result = Gateway.actor.updatePushNotificationToken(account, "xyzzy")
                when (result) {
                    is Result.Success -> {}
                    is Result.Error -> { throw result.exception }
                }
                assertTrue(true)
            }
        }
    }
    */
}
