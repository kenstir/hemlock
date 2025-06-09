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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import net.kenstir.hemlock.data.MapStringAnySerializer

@Serializable
data class XOSRFObject(
    @Serializable(with = MapStringAnySerializer::class)
    val map: Map<String, Any?> = emptyMap(),
    val netClass: String? = null) {

    override fun toString(): String {
        return "XOSRFObject(netClass=$netClass, ${super.toString()})"
    }

}

// Helper extension to extract primitive or keep as JsonElement
private fun JsonElement.jsonPrimitiveOrNull(): Any? = when (this) {
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> boolean
        longOrNull != null -> long
        doubleOrNull != null -> double
        else -> null
    }
    JsonNull -> null
    else -> null
}

