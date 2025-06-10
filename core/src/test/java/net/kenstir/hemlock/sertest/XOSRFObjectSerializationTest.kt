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

import kotlinx.serialization.decodeFromString
import net.kenstir.hemlock.data.jsonMapOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XOSRFObjectSerializationTest {
    @Test
    fun test_encode_classlessObject() {
        val obj = XOSRFObject(jsonMapOf(
            "id" to 42,
            "home_ou" to 69,
            "day_phone" to "508-555-1212",
            "real_null" to null,
        ))
        println("Original:     $obj")

        //val json = Json.encodeToString(XOSRFObject.serializer(), obj)
        val json = Json.encodeToString(obj)
        println("Serialized:   $json")
//
//        //val deserializedObj = Json.decodeFromString(XOSRFObject.serializer(), json)
//        val deserializedObj = Json.decodeFromString<XOSRFObject>(json)
//        println("Deserialized: $deserializedObj")
//
//        assertEquals(obj, deserializedObj)
//        assertNull(deserializedObj.map["real_null"])
    }

    @Test
    fun test_encode_object() {
        val obj = XOSRFObject(jsonMapOf(
            "id" to 42,
            "home_ou" to 69,
            "day_phone" to "508-555-1212",
            "real_null" to null,
        ), "au")
        println("Original:     $obj")

        //val json = Json.encodeToString(XOSRFObject.serializer(), obj)
        val json = Json.encodeToString(obj)
        println("Serialized:   $json")

//        //val deserializedObj = Json.decodeFromString(XOSRFObject.serializer(), json)
//        val deserializedObj = Json.decodeFromString<XOSRFObject>(json)
//        println("Deserialized: $deserializedObj")
//
//        assertEquals(obj, deserializedObj)
//        assertNull(deserializedObj.map["real_null"])
    }
}
