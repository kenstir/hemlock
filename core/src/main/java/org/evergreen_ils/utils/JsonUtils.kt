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

import org.evergreen_ils.data.JSONDictionary
import org.json.JSONTokener

object JsonUtils {
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

//    private fun readSubObject(obj: Any?): Any? {
//        if (obj == null ||
//                obj is String ||
//                obj is Number ||
//                obj is Boolean)
//            return obj
//        return null;
//    }
//
//    @JvmStatic
//    fun parse(jsonString: String): Any? {
//        val jt = JSONTokener(jsonString);
//        return readSubObject(jt.nextValue());
//    }

//    @JvmStatic
//    fun parseObject(jsonString: String): JSONDictionary {
//        val jt = JSONTokener();
//        return readSubObject(jt.nextValue()) as! JSONDictionary;
//    }
}
