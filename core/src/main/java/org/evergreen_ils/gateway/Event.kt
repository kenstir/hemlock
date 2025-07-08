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

import net.kenstir.data.JSONDictionary
import org.evergreen_ils.util.OSRFUtils
import java.util.*

class Event : HashMap<String, Any?> {
    constructor()
    constructor(map: Map<String, Any?>) : super(map)

    val message: String
        get() {
            // This logic is similar to that in place_hold_result.tt2
            textCode?.let { eventKey ->
                eventMessageMap[eventKey]?.let { msg ->
                    return msg
                }
            }
            failPart?.let { failPartKey ->
                failPartMessageMap[failPartKey]?.let { msg ->
                    return msg
                }
            }
            description?.let { msg ->
                if (msg.isNotEmpty()) return msg
            }
            failPart?.let { msg ->
                if (msg.isNotEmpty()) return msg
            }
            textCode?.let { msg ->
                if (msg.isNotEmpty()) return msg
            }
            return "Unknown problem. Contact your local library for further assistance."
        }

    val description: String?
        get() {
            return get("desc") as String?
        }

    val textCode: String?
        get() = get("textcode") as String?

    val failPart: String?
        get() {
            (get("payload") as? OSRFObject)?.let {
                return it["fail_part"] as String?
            }
            return null
        }

    val code: Int
        get() = OSRFUtils.parseInt(get("ilsevent")) ?: 0

    companion object {
        // eventMessageMap is injected
        var eventMessageMap = mapOf<String, String>()
        // failPartMessageMap is injected
        var failPartMessageMap = mapOf<String, String>()

        /**
         * return true if [obj] is an Evergreen "event" (error).
         * See also AppUtils::is_event().
         */
        private fun isEvent(obj: OSRFObject): Boolean {
            val ilsevent = obj.get("ilsevent")
            val textcode = obj.getString("textcode")
            val desc = obj.getString("desc")
            return (ilsevent != null && textcode != null && textcode != "SUCCESS" && desc != null)
        }

        fun parseEvent(payload: Any?): Event? {
            (payload as? OSRFObject)?.let { return parseEvent(it) }
            (payload as? JSONDictionary)?.let {
                // TODO X: I think this is never hit
                return parseEvent(OSRFObject(it))
            }
            return null
        }

        /**
         * return an Event if [obj] is an Evergreen "event" (error) or null if not.
         * See also Account::attempt_hold_placement().
         */
        fun parseEvent(obj: OSRFObject): Event? {
            // case: obj is an event
            if (isEvent(obj)) {
                return Event(obj.map)
            }

            // case: obj has a last_event, or a result with a last_event
            val resultObj = obj.getObject("result")
            val lastEvent = resultObj?.getObject("last_event")
                ?: obj.getObject("last_event")
            if (lastEvent != null) {
                return parseEvent(lastEvent)
            }

            // case: obj has a result which is an event
            if (resultObj != null && isEvent(resultObj)) {
                return Event(resultObj.map)
            }

            // case: obj has a result that is an array of events
            val objList = obj.get("result") as? List<OSRFObject>
            val firstObj = objList?.firstOrNull()
            if (firstObj != null) {
                return parseEvent(firstObj)
            }

            return null
        }
    }
}
