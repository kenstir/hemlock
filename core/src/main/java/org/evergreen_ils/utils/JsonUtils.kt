/*
 * Copyright (c) 2020 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.utils

import kotlinx.serialization.json.Json
import net.kenstir.hemlock.data.JSONDictionary
import net.kenstir.hemlock.data.JSONDictionarySerializer

object JsonUtils {

    // TODO(data): Remove try/catch and let it throw.  At least in the loadStringMap() case, throwing seems better.
    fun parseObject(json: String?): JSONDictionary? {
        return try {
            json?.let { Json.decodeFromString(JSONDictionarySerializer, it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
