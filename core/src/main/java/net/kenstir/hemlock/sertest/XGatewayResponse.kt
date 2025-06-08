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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

enum class XGatewayResponseType {
    UNKNOWN, STRING, OBJECT, ARRAY, EMPTY
}

@Serializable(with = XGatewayResponseSerializer::class)
data class XGatewayResponse(
    val payload: JsonArray,
    val status: Int = 200,
)

object XGatewayResponseSerializer : KSerializer<XGatewayResponse> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XGatewayResponse") {
        element<JsonArray>("payload")
        element<Int>("status", isOptional = true) // optional to make testing easier
    }

    override fun serialize(encoder: Encoder, value: XGatewayResponse) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, JsonElement.serializer(), value.payload)
        composite.encodeIntElement(descriptor, 1, value.status)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): XGatewayResponse {
        val dec = decoder.beginStructure(descriptor)
        var payload: JsonArray? = null
        var status = 200
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> {
                    val element = dec.decodeSerializableElement(descriptor, 0, JsonElement.serializer())
                    payload = element.jsonArrayOrNull()
                        ?: throw SerializationException("payload must be a JSON array")
                }
                1 -> status = dec.decodeIntElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index")
            }
        }
        dec.endStructure(descriptor)
        return XGatewayResponse(payload ?: throw SerializationException("missing payload"), status)
    }
}

// Extension function to safely get JsonArray or null
private fun JsonElement.jsonArrayOrNull(): JsonArray? = this as? JsonArray
