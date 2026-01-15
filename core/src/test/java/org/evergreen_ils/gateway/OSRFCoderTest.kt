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

package org.evergreen_ils.gateway

import org.junit.Before
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import net.kenstir.data.jsonMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OSRFCoderTest {
    @Before
    fun setup() {
        OSRFCoder.clearRegistry()
    }

    @Test
    fun test_register() {
        OSRFCoder.registerClass(
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

        var coder = OSRFCoder.getCoder("xyzzy")
        assertNull(coder)

        coder = OSRFCoder.getCoder("aout")
        assertNotNull(coder)
        assertEquals("aout", coder?.netClass)
        assertEquals(9, coder?.fields?.size)
    }

    // Case: decoding an OSRF object from the wire protocol {"__c": netClass, "__p": [...]}
    @Test
    fun test_decode_wireObject() {
        OSRFCoder.registerClass("test", listOf("can_haz_bacon","id","name"))

        val json = """
            {"__c":"test","__p":["t",1,"Hormel"]}
        """.trimIndent()

        val obj = Json.decodeFromString<OSRFObject>(json)
        println("Deserialized: $obj")
        assertNotNull(obj)
        assertEquals("test", obj.netClass)
        assertEquals(true, obj.getBoolean("can_haz_bacon"))
        assertEquals(1, obj.getInt("id"))
        assertEquals("Hormel", obj.getString("name"))

        // check that "id" is an Int, not a Long.  At this time, the app is not expecting Longs
        assertTrue(obj["id"] is Int)
    }

    // Case: decode an OSRF object when the class hasn't been registered
    @Test
    fun test_decode_wireObject_unregisteredClass() {
        val json = """
            {"__c":"test","__p":["t",1,"Hormel"]}
        """.trimIndent()

        val res = kotlin.runCatching { Json.decodeFromString<OSRFObject>(json) }
        assertTrue(res.isFailure)
        val ex = res.exceptionOrNull()
        println("Exception:    $ex")
        assertEquals("Unregistered class: test", ex?.message)
    }

    // Case: decode an OSRF object from an empty object
    @Test
    fun test_decode_wireObject_empty() {
        val json = "{}"

        val obj = Json.decodeFromString<OSRFObject>(json)
        println("Deserialized: $obj")
        assertNull(obj.netClass)
        assertEquals(0, obj.map.size)
    }

    // Case: decoding an object having 9 fields given an array of only 8 elements.
    // The result should be an OSRFObject with the last field omitted.
    // This happens in the ORG_TYPES_RETRIEVE response, where the last field is omitted.
    @Test
    fun test_decode_shortObject() {
        OSRFCoder.registerClass("test", listOf("juvenile","usrname","home_ou"))

        val json = """
            {"__c":"test","__p":["f","luser"]}
            """.trimIndent()

        val obj = Json.decodeFromString<OSRFObject>(json)
        println("Deserialized: $obj")
        assertNotNull(obj)
        assertEquals(false, obj.getBoolean("juvenile"))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(null, obj.getInt("home_ou"))
    }

    // Case: decode object from array larger than expected
    @Test
    fun test_decode_wireObject_tooManyFields() {
        // 2025-06-10 kenstir: I'm not sure this is still a valid case
    }

    // Case: decoding an array of objects from wire protocol
    @Test
    fun test_decode_wireArray() {
        OSRFCoder.registerClass("mbts", listOf("balance_owed","id","last_billing_ts"))
        OSRFCoder.registerClass("circ", listOf("checkin_lib","checkin_staff","checkin_time"))
        OSRFCoder.registerClass("mvr", listOf("title","author","doc_id"))

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

        val response = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $response")

        assertEquals(true, response.payload[0] is JsonArray)
        val decodedPayload = OSRFCoder.decodePayload(response.payload)
        println("Decoded:      $decodedPayload")

        assertEquals(1, decodedPayload.size)
        val fines = decodedPayload.firstOrNull() as? List<OSRFObject>
        assertEquals(2, fines?.size)
        val firstFine = fines?.get(0)
        assertEquals("circ", firstFine?.getObject("circ")?.netClass ?: "")
        val secondFine = fines?.get(1)
        assertNull(secondFine?.getObject("circ"))
    }

    // Case: decode a recursive object from wire protocol
    @Test
    fun test_decode_orgTree() {
        OSRFCoder.registerClass("aou", listOf("children", "id", "name", "ou_type", "parent_ou", "shortname", "opac_visible", "depth"))
        val json = """
{"payload":[{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[],8,"Example Sub-library 1",4,4,"SL1","t",1]}],4,"Example Branch 1",3,2,"BR1","t",1]},{"__c":"aou","__p":[[],7,"Example Branch 2",3,2,"BR2","t",1]}],2,"Example System 1",2,1,"SYS1","t",1]},{"__c":"aou","__p":[[{"__c":"aou","__p":[[{"__c":"aou","__p":[[],9,"Example Bookmobile 1",5,6,"BM1","t",1]}],9,"Example Branch 3",3,3,"BR3","t",1]},{"__c":"aou","__p":[[],11,"Example Branch 4",3,3,"BR4","t",1]}],3,"Example System 2",2,1,"SYS2","t",1]}],1,"Example Consortium",1,null,"CONS","t",1]}],"status":200}
            """

        val response = Json.decodeFromString<XGatewayResponseContent>(json)
        println("Deserialized: $response")

        val decodedPayload = OSRFCoder.decodePayload(response.payload)
        println("Decoded:      $decodedPayload")

        val obj = GatewayResult.create(json).payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals("CONS", obj?.getString("shortname"))
        val children = obj.getObjectList("children")
        assertEquals(2, children?.size)
        assertEquals("SYS1", children?.get(0)?.getString("shortname"))
    }

    @Test
    fun test_encode_wireObject() {
        OSRFCoder.registerClass("test", listOf("can_haz_bacon","id","name","opt"))

        val obj = OSRFObject(
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
        val actual = Json.encodeToString(OSRFObject.serializer(), obj)
        assertEquals(expected, actual)
    }

    @Test
    fun test_encode_classlessObject() {
        val obj = OSRFObject(jsonMapOf(
            "id" to 42,
            "home_ou" to 69,
            "day_phone" to "508-555-1212",
            "real_null" to null,
        ))
        println("Original:     $obj")

        //val json = Json.encodeToString(OSRFObject.serializer(), obj)
        val json = Json.encodeToString(obj)
        println("Serialized:   $json")

        // If this check proves fragile, we can either change OSRFObjectSerializer to add the keys
        // in field order, or we can just assert some substrings.  I don't think
        // OSRFObjectSerializer.serialize() is used enough to worry about its performance.
        assertEquals(
            """{"id":42,"home_ou":69,"day_phone":"508-555-1212","real_null":null}""",
            json
        )
    }
}
