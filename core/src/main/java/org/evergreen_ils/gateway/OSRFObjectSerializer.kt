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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * This is the kotlinx.serialization way to do serialization.
 *
 * This class is useful for encoding gateway parameters, but it alone is not
 * sufficiently flexible for decoding gateway payloads.  That is because
 * Json.decodeFromString<OSRFObject>() requires that you know you want
 * an XOSRFObject.  When decoding a gateway payload, all you know is that
 * payload is an array.  Thus we ended up with GatewayResponseContent
 * and the deserialization happening in XOSRFCoder.
 */
object OSRFObjectSerializer : KSerializer<OSRFObject> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: OSRFObject) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer only works with JSON")
        jsonEncoder.encodeJsonElement(toJsonObject(value))
    }

    private fun toJsonObject(value: OSRFObject): JsonObject {
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

    private fun serializeAsWireProtocol(value: OSRFObject, netClass: String): JsonObject {
        val coder = OSRFCoder.getCoder(netClass)
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
        is OSRFObject -> toJsonObject(value)
        else -> throw SerializationException("Unsupported type: ${value::class}")
    }

    override fun deserialize(decoder: Decoder): OSRFObject {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This deserializer only works with JSON")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        return OSRFCoder.decodeObject(jsonObject)
    }
}
