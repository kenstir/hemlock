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

package org.evergreen_ils.system

import android.content.res.Resources
import org.evergreen_ils.R
import org.evergreen_ils.utils.JsonUtils
import org.open_ils.Event

/** Emulate the behavior of OPAC messages customized in hold_error_messages.tt2.
 *
 * This class is not used directly; it loads the customizations from app resources
 * and injects them into the Event class.
 *
 * To customize a message by "fail_part", add it to res/raw/fail_part_msg_map.json
 */
object EgMessageMap {
    var initialized = false
    var failPartMessageMap = mutableMapOf<String, String>()

    fun init(resources: Resources) {
        synchronized(this) {
            if (initialized) return

            Event.failPartMessageMap.clear()

            // load map from json resource
            val inputStream = resources.openRawResource(R.raw.fail_part_msg_map)
            val contents = inputStream.bufferedReader().use { it.readText() }
            val dict = JsonUtils.parseObject(contents) ?: mapOf()
            for ((k, v) in dict) {
                (v as? String)?.let {
                    Event.failPartMessageMap[k] = it
                }
            }

            initialized = true
        }
    }
}
