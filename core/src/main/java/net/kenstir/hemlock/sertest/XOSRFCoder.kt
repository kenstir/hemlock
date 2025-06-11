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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/*** [OSRFCoder] is used to decode OSRF objects from OpenSRF wire format. */
class XOSRFCoder(val netClass: String, val fields: List<String>) {


    companion object {
        var registry: HashMap<String, XOSRFCoder> = HashMap()

        fun clearRegistry() {
            registry.clear()
        }

        fun registerClass(netClass: String, fields: List<String>) {
            registry[netClass] = XOSRFCoder(netClass, fields)
        }

        fun getCoder(netClass: String): XOSRFCoder? {
            return registry[netClass]
        }

        /** decode an OSRF payload in wire format
         *
         * in the process, flesh out any OSRFObjects in wire format
         */
        fun decodePayload(elementList: JsonArray): List<Any?> {
            return decodeArray(elementList)
        }

        private fun decodeArray(elementList: JsonArray): List<Any?> {
            var decoded = mutableListOf<Any?>()
            for (element in elementList) {
                decoded.add(decodeElement(element))
            }
            return decoded
        }

        private fun decodeElement(element: JsonElement): Any? {
            return when (element) {
                is JsonNull -> null
                is JsonPrimitive -> decodePrimitive(element)
                is JsonObject -> decodeObject(element)
                is JsonArray -> decodeArray(element)
                else -> throw XDecodingException("unsupported element type: ${element::class.simpleName}")
            }
        }

        private fun decodeObject(element: JsonObject): Any? {
            if (element.containsKey("__c") && element.containsKey("__p")) {
                // wire protocol object
                val netClass = element["__c"]?.jsonPrimitive?.content
                    ?: throw XDecodingException("missing __c field in wire protocol object")
                val coder = getCoder(netClass)
                    ?: throw XDecodingException("unregistered class: $netClass")
                val values = element["__p"]?.jsonArray
                    ?: throw XDecodingException("missing __p field in wire protocol object")
                if (values.size != coder.fields.size) {
                    throw XDecodingException("field count mismatch for class $netClass")
                }
                val map = mutableMapOf<String, Any?>()
                for (i in coder.fields.indices) {
                    map[coder.fields[i]] = decodeElement(values[i])
                }
                return XOSRFObject(map, netClass)
            } else {
                // regular object
                return element.mapValues { decodeElement(it.value) }
            }
        }

        private fun decodePrimitive(element: JsonPrimitive): Any? {
            return when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> throw XDecodingException("unsupported element type: ${element::class.simpleName}")
            }
        }
    }
}
