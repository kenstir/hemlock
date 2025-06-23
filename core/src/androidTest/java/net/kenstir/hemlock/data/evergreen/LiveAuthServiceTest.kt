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

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.android.StdoutLogProvider
import net.kenstir.hemlock.net.InitServiceOptions
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.model.EvergreenAccount
import org.evergreen_ils.net.EvergreenAuthService
import org.evergreen_ils.net.EvergreenInitService
import org.evergreen_ils.net.EvergreenUserService
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class LiveAuthServiceTest {
    companion object {
        val authService = EvergreenAuthService()
        val initializationService = EvergreenInitService()
        val userService = EvergreenUserService()

        // See root build.gradle for notes on customizing instrumented test variables (hint: secret.gradle)
        val testServer = getRequiredArg("server")
        val testUsername = getRequiredArg("username")
        val testPassword = getRequiredArg("password")

        var account = EvergreenAccount(testUsername)
        var isServiceDataLoaded = false

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            XGatewayClient.baseUrl = testServer
            XGatewayClient.clientCacheKey = "42"
        }

        fun getRequiredArg(name: String): String {
            return InstrumentationRegistry.getArguments().getString(name) ?: throw RuntimeException("Missing required arg: $name")
        }
    }

    suspend fun getTestAuthToken(): Result<String> {
        return authService.getAuthToken(testUsername, testPassword)
    }

    suspend fun loadTestServiceData(): Result<Unit> {
        if (isServiceDataLoaded) return Result.Success(Unit)
        val result = initializationService.loadServiceData(InitServiceOptions("42", true))
        isServiceDataLoaded = true
        return result
    }

    @Test
    fun test_getAuthToken() = runTest {
        val result = getTestAuthToken()
        println("Result: $result")
        assertTrue(result.succeeded)

        val authToken = result.get()
        assertTrue(authToken.isNotEmpty())
    }

    @Test
    fun loadServiceData() = runTest {
        val result = loadTestServiceData()
        println("Result: $result")
        assertTrue(result.succeeded)
    }
}
