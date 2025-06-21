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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import net.kenstir.hemlock.data.JSONDictionary

/** a parameter for requests through the OSRF Gateway */
@Serializable(with = GatewayParamSerializer::class)
data class GatewayParam(val value: Any?)

fun paramListOf(vararg elements: Any?): List<GatewayParam> =
    ArrayList(elements.map { GatewayParam(it) })

object GatewayParamSerializer : kotlinx.serialization.KSerializer<GatewayParam> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("GatewayParam", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): GatewayParam {
        // not used
        return GatewayParam(decoder.decodeString())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: GatewayParam) {
        when (value.value) {
            null -> encoder.encodeNull()
            is String -> encoder.encodeString(value.value)
            is Int -> encoder.encodeInt(value.value)
            is Boolean -> encoder.encodeBoolean(value.value)
            is Double -> encoder.encodeDouble(value.value)
            is Float -> encoder.encodeFloat(value.value)
            is Long -> encoder.encodeLong(value.value)
            is Map<*, *> -> {
                val jsonDict = value.value as JSONDictionary
                encoder.encodeSerializableValue(XOSRFObject.serializer(), XOSRFObject(jsonDict))
            }
            is XOSRFObject -> {
                encoder.encodeSerializableValue(XOSRFObject.serializer(), value.value as XOSRFObject)
            }
            is List<*> -> {
                val jsonArray = value.value.map { GatewayParam(it) }
                encoder.encodeSerializableValue(ListSerializer(GatewayParam.serializer()), jsonArray)
            }
            else -> throw SerializationException("Unsupported type for GatewayParam: ${value.value::class.simpleName}")
        }
    }
}
