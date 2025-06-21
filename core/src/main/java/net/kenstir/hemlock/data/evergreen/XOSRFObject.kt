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

import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.math.min

@Serializable(with = XOSRFObjectSerializer::class)
data class XOSRFObject(
    val map: Map<String, Any?> = emptyMap(),
    val netClass: String? = null)
{
    override fun toString(): String {
        return "XOSRFObject(netClass=$netClass, map${map.toString()})"
    }

    operator fun get(key: String): Any? {
        return map[key]
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return map[key] as? String ?: defaultValue
    }

    fun getInt(key: String): Int? {
        return when (val value = map[key]) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    fun getBoolean(key: String): Boolean {
        return when (val value = map[key]) {
            is Boolean -> value
            is String -> value == "t"
            else -> false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getObject(key: String): XOSRFObject? {
        return when (val value = map[key]) {
            is XOSRFObject -> value
            is Map<*, *> -> XOSRFObject(value as Map<String, Any?>)
            else -> null
        }
    }

    fun getDate(key: String): Date? {
        return OSRFUtils.parseDate(getString(key))
    }

    fun getAny(key: String): Any? {
        return map[key]
    }
}

// this is the kotlinx.serialization way to do it
//
// It is useful for encoding but not sufficiently flexible for decoding gateway payloads
object XOSRFObjectSerializer : KSerializer<XOSRFObject> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: XOSRFObject) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer only works with JSON")

        value.netClass?.let {
            serializeWireProtocol(jsonEncoder, value, it)
        } ?: run {
            // If netClass is not present, serialize as a map
            val jsonObject = buildJsonObject {
                for ((key, v) in value.map) {
                    put(key, toJsonElement(v))
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }

    private fun serializeWireProtocol(jsonEncoder: JsonEncoder, value: XOSRFObject, netClass: String) {
        val coder = XOSRFCoder.getCoder(netClass)
            ?: throw SerializationException("Unregistered class: $netClass")

        val jsonValues = buildJsonArray {
            for (key in coder.fields) {
                add(toJsonElement(value[key]))
            }
        }
        val jsonObject = JsonObject(mapOf(
            "__c" to JsonPrimitive(netClass),
            "__p" to jsonValues
        ))
        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): XOSRFObject {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This deserializer only works with JSON")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        if ("__c" in jsonObject) {
            // Deserialize as wire protocol object
            return deserializeWireProtocol(jsonObject)
        } else {
            // Deserialize as a regular map
            return XOSRFObject(jsonObject.mapValues { fromJsonElement(it.value) })
        }
    }

    // TODO: why do we have both XOSRFObjectSerializer.deserializeWireProtocol and XOSRFCoder.decodeObject?
    // They seem almost identical
    // It seems to be used only in our unit tests
    fun deserializeWireProtocol(jsonObject: JsonObject): XOSRFObject {
        val netClass = jsonObject["__c"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing __c field in wire protocol object")
        val coder = XOSRFCoder.getCoder(netClass)
            ?: throw SerializationException("Unregistered class: $netClass")
        val values = jsonObject["__p"]?.jsonArray
            ?: throw SerializationException("Missing __p field in wire protocol object")
        if (values.size > coder.fields.size) {
            throw SerializationException("Field count mismatch for class $netClass (expected ${coder.fields.size}, got ${values.size})")
        }
        val map = HashMap<String, Any?>(coder.fields.size)
        val count = min(coder.fields.size, values.size)
        for (i in 0 until count) {
            map[coder.fields[i]] = fromJsonElement(values[i])
        }
        return XOSRFObject(map, netClass)
    }

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = value as? Map<String, Any?>
                ?: throw SerializationException("Only Map<String, Any?> is supported")
            JsonObject(map.mapValues { toJsonElement(it.value) })
        }
        else -> throw SerializationException("Unsupported type: ${value::class}")
    }

    private fun fromJsonElement(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> {
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.longOrNull != null -> {
                    val long = element.long
                    if (long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt() else long
                }
                element.doubleOrNull != null -> element.double
                else -> element.content
            }
        }
        is JsonObject -> {
            element.mapValues { fromJsonElement(it.value) }
        }
        is JsonArray -> element.map { fromJsonElement(it) }
    }
}
