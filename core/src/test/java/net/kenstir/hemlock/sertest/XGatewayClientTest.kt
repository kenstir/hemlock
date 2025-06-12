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

package net.kenstir.hemlock.sertest

import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.evergreen.XOSRFCoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class XGatewayClientTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            // TODO: get this from gradle config
            XGatewayClient.baseUrl = "https://gapines.org/osrf-gateway-v1"
        }
    }

     @Test
     fun test_fetchServerVersion() = runTest {
         val response = XGatewayClient.fetchServerVersion()
         println("Response: $response")
         assertNotNull(response)

         val decodedPayload = XOSRFCoder.decodePayload(response.payload)
         println("Decoded:      $decodedPayload")
         assertEquals(1, decodedPayload.size)
         assertTrue(decodedPayload[0] is String)
     }
}
