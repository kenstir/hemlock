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

import org.junit.Before
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import net.kenstir.hemlock.data.JSONDictionary
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
        assertEquals("test", obj.netClass)
        assertEquals(true, obj.getBoolean("can_haz_bacon"))
        assertEquals(1, obj.getInt("id"))
        assertEquals("Hormel", obj.getString("name"))
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
        println("Exception:    $ex")
        assertEquals("unregistered class: test", ex?.message)
    }

    // Case: decode an OSRF object from an empty object
    @Test
    fun test_decode_wireObject_empty() {
        val json = "{}"

        val obj = Json.decodeFromString<XOSRFObject>(json)
        println("Deserialized: $obj")
        assertNull(obj.netClass)
        assertEquals(0, obj.map.size)
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

    // Case: decoding an array of objects from wire protocol
    @Test
    fun test_decode_wireArray() {
        XOSRFCoder.registerClass("mbts", listOf("balance_owed","id","last_billing_ts"))
        XOSRFCoder.registerClass("circ", listOf("checkin_lib","checkin_staff","checkin_time"))
        XOSRFCoder.registerClass("mvr", listOf("title","author","doc_id"))

        val json = """
{"payload":[[
                {
                    "transaction":{"__c":"mbts","__p":["1.15",182746988,"2018-01-10T23:59:59-0500"]},
                    "circ":{"__c":"circ","__p":[66,1175852,"2018-01-10T16:32:17-0500"]},
                    "record":{"__c":"mvr","__p":["Georgia adult literacy resources manual","State Bar of Georgia",1475710]},
                    "copy":null
                },
                {
                    "transaction":{"__c":"mbts","__p":["0.10",174615422,"2017-05-01T14:03:24-0400"]}
                }
            ]],"status":200}
        """.trimIndent()

        val response = Json.decodeFromString<XGatewayResponse>(json)
        println("Deserialized: $response")

        assertEquals(true, response.payload[0] is JsonArray)
        val decodedPayload = XOSRFCoder.decodePayload(response.payload)
        println("Decoded:      $decodedPayload")

        assertEquals(1, decodedPayload.size)
        val listOfFines = decodedPayload[0] as? List<*>
        val firstItem = listOfFines?.get(0) as? JSONDictionary
        val transactionObj = firstItem?.get("transaction") as? XOSRFObject
        assertNotNull(transactionObj)
        assertEquals("1.15", transactionObj?.getString("balance_owed"))
    }

    // Case: decode a recursive object from wire protocol
    @Test
    fun test_decode_orgTree() {
        XOSRFCoder.registerClass("aou", listOf("children", "id", "name", "ou_type", "parent_ou", "shortname", "opac_visible", "depth"))
        val json = """
{"payload":[{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[],8,"Example Sub-library 1",4,4,"SL1","t",1]}],4,"Example Branch 1",3,2,"BR1","t",1]},{"__c":"aou","__p":[[],7,"Example Branch 2",3,2,"BR2","t",1]}],2,"Example System 1",2,1,"SYS1","t",1]},{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[],9,"Example Bookmobile 1",5,6,"BM1","t",1]}],9,"Example Branch 3",3,3,"BR3","t",1]},{"__c":"aou","__p":[[],11,"Example Branch 4",3,3,"BR4","t",1]}],3,"Example System 2",2,1,"SYS2","t",1]}],1,"Example Consortium",1,null,"CONS","t",1]}],"status":200}
            """

        val response = Json.decodeFromString<XGatewayResponse>(json)
        println("Deserialized: $response")

        val decodedPayload = XOSRFCoder.decodePayload(response.payload)
        println("Decoded:      $decodedPayload")

        val obj = decodedPayload[0] as? XOSRFObject
        assertNotNull(obj)
        assertEquals("CONS", obj?.getString("shortname"))
        val children = obj?.getAny("children") as? List<XOSRFObject>
        assertEquals(2, children?.size)
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
