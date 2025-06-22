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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayResponseContentTest {

    @Test
    fun test_decode_emptyPayload() {
        val json = """
            {"payload":[],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(0, resp.payload.size)
    }

    @Test
    fun test_decode_emptyPayloadWithReversedKeys() {
        val json = """
            {"status":200,"payload":[]}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(0, resp.payload.size)
    }

    @Test
    fun test_decode_emptyList() {
        val json = """
            {"payload":[[]],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)
        assertEquals(emptyList<JsonElement>(), resp.payload[0].jsonArray)
    }

    @Test
    fun test_decode_string() {
        val json = """
            {"payload":["3-7-4"],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)
        assertTrue(resp.payload[0].jsonPrimitive.isString)
        assertEquals("3-7-4", resp.payload[0].jsonPrimitive.content)
    }

    @Test
    fun test_decode_object() {
        val json = """
            {"payload":[{"hemlock.cache_key":null}],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)

        val obj = resp.payload[0].jsonObject
        val value = obj["hemlock.cache_key"]
        assertEquals(JsonNull, value)
    }

    @Test
    fun test_decode_nestedObject() {
        val json = """
            {"payload":[{"ilsevent":0,"textcode":"SUCCESS","desc":"Success","payload":{"authtoken":"***","authtime":1209600}}],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)

        val obj = resp.payload[0].jsonObject
        assertEquals("SUCCESS", obj["textcode"]?.jsonPrimitive?.content)
        val subPayload = obj["payload"]?.jsonObject
        assertNotNull(subPayload)
        assertEquals("***", subPayload?.get("authtoken")?.jsonPrimitive?.content)
    }

    @Test
    fun test_decode_OSRFWireObject() {
        val json = """
            {"payload":[{"__c":"cbreb","__p":[[],"bookbag",2958647,"books to read",null,1826347,"f","2025-01-04T01:08:16-0500",null]}],"status":200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")
    }

    @Test
    fun test_decode_nestedOSRFWireObject() {
        val json = """
            {"payload": [{"__c": "aou", "__p": [[{"__c": "aou", "__p": [[{"__c": "aou", "__p": [[], 11, 12, 7, 12, 10, "Example Branch 4", 3, 3, "BR4", "br4@example.com", "(555) 555-0274", "t", 1]}], 3, 3, 3, 3, 3, "Example System 2", 2, 1, "SYS2", null, null, "t", 1]}], 1, 1, 1, 1, 1, "Example Consortium", 1, null, "CONS", null, null, "t", 1]}], "status": 200}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $resp")

        assertNotNull(resp)
        assertEquals(1, resp.payload.size)

        val obj = resp.payload[0].jsonObject
        val nestedPayload = obj["__p"]?.jsonArray?.getOrNull(0)
        assertNotNull(nestedPayload)
    }

    // the OSRF gateway adds a debug message if the request dies
    @Test
    fun test_decode_serverCrash() {
        val json = """
            {"payload":[],"debug": "osrfMethodException : ...","status":500}
        """.trimIndent()

        val resp = Json.decodeFromString<XGatewayResponseContent>(json)
        assertEquals("osrfMethodException : ...", resp.debug)
    }

    // Test to handle https://bugs.launchpad.net/opensrf/+bug/1883169
    @Test
    fun test_decode_gatewayErrorResponseIsNotValidJSON() {
        val jsonIsh = """
            {"payload":[],"debug": "...NULL,NULL F,"status":500}
            """

        try {
            Json.decodeFromString<XGatewayResponseContent>(jsonIsh)
            assertTrue("Expected SerializationException", false)
        } catch (e: Exception) {
            assertTrue(e is SerializationException)
        }
    }
}
