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

import org.junit.Before
import kotlinx.serialization.json.Json
import net.kenstir.hemlock.data.jsonMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XOSRFCoderTests {
    @Before
    fun setup() {
        XOSRFCoder.clearRegistry()
    }

    @Test
    fun test_register() {
        XOSRFCoder.registerClass(
            "aout",
            listOf(
                "children",
                "can_have_users",
                "can_have_vols",
                "depth",
                "id",
                "name",
                "opac_label",
                "parent",
                "org_units"
            )
        )

        var coder = XOSRFCoder.getCoder("xyzzy")
        assertNull(coder)

        coder = XOSRFCoder.getCoder("aout")
        assertNotNull(coder)
        assertEquals("aout", coder?.netClass)
        assertEquals(9, coder?.fields?.size)
    }

    // Case: decoding an OSRF object from the wire protocol {"__c": netClass, "__p": [...]}
    @Test
    fun test_decode_wireObject() {
        XOSRFCoder.registerClass("test", listOf("can_haz_bacon","id","name"))

        val json = """
            {"__c":"test","__p":["t",1,"Hormel"]}
        """.trimIndent()

        val obj = Json.decodeFromString<XOSRFObject>(json)
        println("Deserialized: $obj")
        assertNotNull(obj)
    }

    // Case: decode an OSRF object when the class hasn't been registered
    @Test
    fun test_decode_wireObject_unregisteredClass() {
        val json = """
            {"__c":"test","__p":["t",1,"Hormel"]}
        """.trimIndent()

        val res = kotlin.runCatching { Json.decodeFromString<XOSRFObject>(json) }
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull()
        println("Exception: $ex")
        assertEquals("unregistered class: test", ex?.message)
    }

    // Case: decode an OSRF object from an empty object
    @Test
    fun test_decode_wireObject_empty() {
    }

    // Case: decoding an object having 9 fields given an array of only 8 elements.
    // The result should be an OSRFObject with the last field omitted.
    @Test
    fun test_decode_shortObject() {
        // 2025-06-10 kenstir: I'm not sure this is still a valid case
    }

    // Case: decode object from array larger than expected
    @Test
    fun test_decode_wireObject_tooManyFields() {
        // 2025-06-10 kenstir: I'm not sure this is still a valid case
    }

    // Case: decoding an array of OSRF objects from wire protocol
    @Test
    fun test_decode_wireArray() {
    }

    // Case: decode a recursive object from wire protocol
    @Test
    fun test_decode_orgTree() {
    }

    @Test
    fun test_encode_wireObject() {
        XOSRFCoder.registerClass("test", listOf("can_haz_bacon","id","name","opt"))

        val obj = XOSRFObject(
            jsonMapOf(
                "can_haz_bacon" to true,
                "id" to 1,
                "name" to "Hormel",
                "opt" to null
            ),
            "test"
        )

        val expected = """
            {"__c":"test","__p":[true,1,"Hormel",null]}
        """.trimIndent()
        val actual = Json.encodeToString(XOSRFObject.serializer(), obj)
        assertEquals(expected, actual)
    }
}
