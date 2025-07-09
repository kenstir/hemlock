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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min

/*** [OSRFCoder] is used to decode OSRF objects from OpenSRF wire format. */
class OSRFCoder(val netClass: String, val fields: List<String>) {

    companion object {
        var registry: HashMap<String, OSRFCoder> = HashMap()

        fun clearRegistry() {
            registry.clear()
        }

        @JvmStatic
        fun registerClass(netClass: String, fields: List<String>) {
            registry[netClass] = OSRFCoder(netClass, fields)
        }

        fun getCoder(netClass: String): OSRFCoder? {
            return registry[netClass]
        }

        /** decode an OSRF payload in wire format
         *
         * in the process, flesh out any OSRFObjects in wire format
         */
        fun decodePayload(elements: JsonArray): List<Any?> {
            return decodeArray(elements)
        }

        fun decodeArray(elements: JsonArray): List<Any?> {
            val size = elements.size
            val decoded = ArrayList<Any?>(size)
            for (i in 0 until size) {
                decoded.add(decodeElement(elements[i]))
            }
            return decoded
        }

        fun decodeElement(element: JsonElement): Any? {
            return when (element) {
                is JsonNull -> null
                is JsonPrimitive -> decodePrimitive(element)
                is JsonObject -> decodeObject(element)
                is JsonArray -> decodeArray(element)
            }
        }

        fun decodePrimitive(element: JsonPrimitive): Any {
            return when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.intOrNull != null -> element.int
                // At this point, the app is using Int everywhere, not Long
                //element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> throw DecodingException("Unsupported element type: ${element::class.simpleName}")
            }
        }

        fun decodeObject(element: JsonObject): OSRFObject {
            return if (element.containsKey("__c") && element.containsKey("__p")) {
                // wire protocol object
                decodeObjectFromWireProtocol(element)
            } else {
                // regular object, return XOSRFObject for compatibility with old code
                OSRFObject(element.mapValues { decodeElement(it.value) })
            }
        }

        private fun decodeObjectFromWireProtocol(jsonObject: JsonObject): OSRFObject {
            val netClass = jsonObject["__c"]?.jsonPrimitive?.content
                ?: throw DecodingException("Missing __c field in wire protocol object")
            val coder = getCoder(netClass)
                ?: throw DecodingException("Unregistered class: $netClass")
            val values = jsonObject["__p"]?.jsonArray
                ?: throw DecodingException("Missing __p field in wire protocol object")
            if (values.size > coder.fields.size) {
                throw DecodingException("Field count mismatch for class $netClass (expected ${coder.fields.size}, got ${values.size})")
            }
            val map = HashMap<String, Any?>(coder.fields.size)
            val count = min(coder.fields.size, values.size)
            for (i in 0 until count) {
                map[coder.fields[i]] = decodeElement(values[i])
            }
            return OSRFObject(map, netClass)
        }
    }
}
