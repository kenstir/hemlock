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

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.data.Account
import org.evergreen_ils.net.Volley
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class SerializationTest {

    companion object {
        private val TAG = SerializationTest::class.java.simpleName

        lateinit var args: Bundle
        lateinit var server: String
        lateinit var username: String
        lateinit var password: String
        lateinit var account: Account

        var authToken = ""

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
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            if (authToken.isEmpty()) return
        }

        @JvmStatic
        suspend fun getSession() {
        }
    }

    @Test
    fun test_basic() {
        val dataList = listOf(
            XData(1, "one"),
            XData(2, "two"),
        )
        val json = Json.encodeToString(dataList)
        Log.d(TAG, "Serialized JSON: $json")

        val deserializedList = Json.decodeFromString<List<XData>>(json)
        Log.d(TAG, "Deserialized List: $deserializedList")

        assertNotNull(deserializedList)
        assertTrue(deserializedList.isNotEmpty())
        assertTrue(deserializedList[0].id == 1 && deserializedList[0].name == "one")
        assertTrue(deserializedList[1].id == 2 && deserializedList[1].name == "two")
    }

    @Test
    fun test_gateway_response_empty() {
        val json = """
            {"payload":[],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponse>(json)
        Log.d(TAG, "Deserialized Gateway Response: $resp")

        assertNotNull(resp)
        assertEquals(0, resp.payload.size)
    }

    @Test
    fun test_gateway_response_emptyList() {
        val json = """
            {"payload":[[]],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponse>(json)
        Log.d(TAG, "Deserialized Gateway Response: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)
    }
}
