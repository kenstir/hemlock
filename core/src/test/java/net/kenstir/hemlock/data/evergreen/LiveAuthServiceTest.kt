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

import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.AuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class LiveAuthServiceTest {
    companion object {
        val authService = EvergreenAuthService()

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val testServer = System.getProperty("testEvergreenServer")
            val testUsername = System.getProperty("testEvergreenUsername")
            val testPassword = System.getProperty("testEvergreenPassword")

            XGatewayClient.baseUrl = testServer
        }
    }

     @Test
     fun test_fetchServerVersion() = runTest {
         val result = authService.fetchServerVersion()
         println("Result: $result")
         assertTrue(result.succeeded)

         val version = result.get()
         assertNotNull(version)
     }
}
