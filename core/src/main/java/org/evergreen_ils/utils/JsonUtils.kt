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

import net.kenstir.hemlock.data.JSONDictionary
import org.json.JSONObject
import org.opensrf.util.JSONException
import org.opensrf.util.JSONReader

object JsonUtils {
    /** Parse a metarecord hold attribute "holdable_formats" into a list of ccvm codes */
    @JvmStatic
    fun parseHoldableFormats(dict: JSONDictionary?): ArrayList<String> {
        var formats = ArrayList<String>()
        if (dict == null)
            return formats
        for ((_, v) in dict) {
            val l = v as? ArrayList<*>
            if (l != null) {
                for (elem in l) {
                    val e = elem as? Map<String, String>
                    val attr = e?.get("_attr")
                    val value = e?.get("_val")
                    if (attr == "mr_hold_format" && value != null) {
                        formats.add(value)
                    }
                }
            }
        }
        return formats
    }

    @JvmStatic
    fun parseObject(json: String?): JSONDictionary? {
        return try {
            JSONReader(json).readObject()
        } catch (e: JSONException) {
            null
        }
    }

    @JvmStatic
    fun toJSONString(obj: JSONDictionary): String {
        val jsonObject = JSONObject(obj)
        return jsonObject.toString()
    }
}
