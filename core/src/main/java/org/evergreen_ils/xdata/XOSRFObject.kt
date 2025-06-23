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

package org.evergreen_ils.xdata

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
import org.evergreen_ils.data.OSRFUtils

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

/**
 * This is the kotlinx.serialization way to do serialization.
 *
 * This class is useful for encoding gateway parameters, but it alone is not
 * sufficiently flexible for decoding gateway payloads.  That is because
 * Json.decodeFromString<XOSRFObject>() requires that you know you want
 * an XOSRFObject.  When decoding a gateway payload, all you know is that
 * payload is an array.  Thus we ended up with GatewayResponseContent
 * and the deserialization happening in XOSRFCoder.
 */
object XOSRFObjectSerializer : KSerializer<XOSRFObject> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: XOSRFObject) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer only works with JSON")
        jsonEncoder.encodeJsonElement(toJsonObject(value))
    }

    private fun toJsonObject(value: XOSRFObject): JsonObject {
        value.netClass?.let {
            return serializeAsWireProtocol(value, it)
        } ?: run {
            // If netClass is not present, serialize as a map
            return buildJsonObject {
                for ((key, v) in value.map) {
                    put(key, toJsonElement(v))
                }
            }
        }
    }

    private fun serializeAsWireProtocol(value: XOSRFObject, netClass: String): JsonObject {
        val coder = XOSRFCoder.getCoder(netClass)
            ?: throw SerializationException("Unregistered class: $netClass")

        val jsonValues = buildJsonArray {
            for (key in coder.fields) {
                add(toJsonElement(value[key]))
            }
        }
        return JsonObject(mapOf(
            "__c" to JsonPrimitive(netClass),
            "__p" to jsonValues
        ))
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
        is XOSRFObject -> toJsonObject(value)
        else -> throw SerializationException("Unsupported type: ${value::class}")
    }

    override fun deserialize(decoder: Decoder): XOSRFObject {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This deserializer only works with JSON")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        return XOSRFCoder.decodeObject(jsonObject)
    }
}
