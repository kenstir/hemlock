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

package org.evergreen_ils

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.net.LoadStartupOptions
import net.kenstir.hemlock.net.ServiceConfig
import org.evergreen_ils.model.EvergreenAccount
import org.evergreen_ils.net.EvergreenAuthService
import org.evergreen_ils.net.EvergreenBiblioService
import org.evergreen_ils.net.EvergreenCircService
import org.evergreen_ils.net.EvergreenLoaderService
import org.evergreen_ils.net.EvergreenOrgService
import org.evergreen_ils.net.EvergreenSearchService
import org.evergreen_ils.net.EvergreenUserService
import org.evergreen_ils.xdata.XGatewayClient
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class LiveAuthServiceTest {
    companion object {
        val serviceConfig = ServiceConfig(
            EvergreenLoaderService,
            EvergreenAuthService,
            EvergreenBiblioService,
            EvergreenCircService,
            EvergreenSearchService,
            EvergreenUserService,
            EvergreenOrgService,
        )

        // See root build.gradle for notes on customizing instrumented test variables (hint: secret.gradle)
        val testServer = getRequiredArg("server")
        val testUsername = getRequiredArg("username")
        val testPassword = getRequiredArg("password")

        var account = EvergreenAccount(testUsername)
        var isServiceDataLoaded = false
        var isSessionLoaded = false

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            XGatewayClient.baseUrl = testServer
            XGatewayClient.clientCacheKey = "42"
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            println("Tearing down LiveAuthServiceTest: isSessionLoaded=$isSessionLoaded")
            if (!isSessionLoaded) return
            runBlocking {
                launch(Dispatchers.Main) {
                    val result = serviceConfig.userService.deleteSession(account)
                    when (result) {
                        is Result.Error -> Log.d("LiveAuthServiceTest", "Error deleting session", result.exception)
                        is Result.Success -> Log.d("LiveAuthServiceTest", "Session deleted successfully")
                    }
                }
            }
        }

        fun getRequiredArg(name: String): String {
            return InstrumentationRegistry.getArguments().getString(name) ?: throw RuntimeException("Missing required arg: $name")
        }
    }

    suspend fun loadTestAuthToken(): Result<Unit> {
        if (account.authToken != null) return Result.Success(Unit)
        val result = serviceConfig.authService.getAuthToken(testUsername, testPassword)
        when (result) {
            is Result.Error -> return result
            is Result.Success -> {}
        }
        account.authToken = result.get()
        return Result.Success(Unit)
    }

    suspend fun loadTestServiceData(): Result<Unit> {
        if (isServiceDataLoaded) return Result.Success(Unit)
        val result = serviceConfig.loaderService.loadStartupPrerequisites(LoadStartupOptions("42", true))
        isServiceDataLoaded = true
        return result
    }

    suspend fun loadTestUserSession(): Result<Unit> {
        if (isSessionLoaded) return Result.Success(Unit)
        val authResult = loadTestAuthToken()
        if (!authResult.succeeded) return authResult
        val result = serviceConfig.userService.loadUserSession(account)
        isSessionLoaded = true
        return result
    }

    @Test
    fun test_getAuthToken() = runTest {
        val result = loadTestAuthToken()
        println("Result: $result")
        assertTrue(result.succeeded)

        assertNotNull(account.authToken)
    }

    @Test
    fun test_loadServiceData() = runTest {
        val result = loadTestServiceData()
        println("Result: $result")
        assertTrue(result.succeeded)
    }

    @Test
    fun test_loadUserSession() = runTest {
        loadTestAuthToken()
        loadTestServiceData()

        val result = loadTestUserSession()
        println("Result: $result")
        assertTrue(result.succeeded)

        assertNotNull(account.id)
        assertNotNull(account.authToken)
    }
}
