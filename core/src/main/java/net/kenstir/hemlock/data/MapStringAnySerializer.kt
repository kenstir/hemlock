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

package net.kenstir.hemlock.data

import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@OptIn(ExperimentalSerializationApi::class)
object MapStringAnySerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer only works with JSON")

        val jsonObject = buildJsonObject {
            for ((key, v) in value) {
                put(key, toJsonElement(v))
            }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This deserializer only works with JSON")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        return jsonObject.mapValues { fromJsonElement(it.value) }
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
        is JsonObject -> element.mapValues { fromJsonElement(it.value) }
        is JsonArray -> element.map { fromJsonElement(it) }
    }
}
