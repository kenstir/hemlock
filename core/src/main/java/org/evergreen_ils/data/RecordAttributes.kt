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
package org.evergreen_ils.data

import org.evergreen_ils.gateway.OSRFObject

object RecordAttributes {
    fun parseAttributes(mraObj: OSRFObject?): HashMap<String, String> {
        if (mraObj == null) return HashMap()
        val attrs = mraObj.getString("attrs", null)
        if (attrs.isNullOrEmpty()) return HashMap()
        return parseAttributes(attrs)
    }

    fun parseAttributes(attrs_dump: String?): HashMap<String, String> {
        val map = HashMap<String, String>()
        if (attrs_dump.isNullOrEmpty()) return map
        val attributeList = attrs_dump.split(Regex("(?<=\"), (?=\")"))
        for (item in attributeList) {
            val kv = item.split("=>")
            val k = kv[0].replace("\"", "")
            val v = kv[1].replace("\"", "")
            map[k] = v
        }
        return map
    }
}
