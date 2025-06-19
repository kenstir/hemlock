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

import kotlinx.serialization.json.Json
import net.kenstir.hemlock.data.jsonMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayParamTest {
    @Test
    fun test_paramArrayOf() {
        val params = paramListOf("test", 123, true, null)
        assertTrue(params is ArrayList<GatewayParam>)
        assertEquals(4, params.size)
        assertEquals("test", params[0].value)
        assertEquals(123, params[1].value)
        assertEquals(true, params[2].value)
        assertEquals(null, params[3].value)
    }

    @Test
    fun test_gatewayParamSerialization_primitives() {
        val tests = mapOf<String, Any?>(
            "\"test\"" to "test",
            "123" to 123,
            "true" to true,
            "null" to null
        )
        for ((expected, value) in tests) {
            val json = Json.encodeToString(GatewayParam(value))
            println("Serialized: $json")
            assertEquals(expected, json)
        }
    }

    @Test
    fun test_serialization_XOSRFObject() {
        val obj = XOSRFObject(jsonMapOf(
            "id" to 1,
            "name" to "Yanni",
            "juvenile" to "t",
            "optional" to null,
        ))
        val json = Json.encodeToString(obj)
        println("Serialized: $json")
        assertEquals("""
            {"id":1,"name":"Yanni","juvenile":"t","optional":null}
        """.trimIndent(), json)
    }
}