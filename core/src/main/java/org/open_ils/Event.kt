package org.open_ils

import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.data.JSONDictionary
import org.opensrf.util.OSRFObject
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
            (get("payload") as? JSONDictionary)?.let {
                return it["fail_part"] as String?
            }
            return null
        }

    val code: Int
        get() = OSRFUtils.parseInt(get("ilsevent")) ?: 0

    companion object {
        // eventMessageMap is injected
        var eventMessageMap = mutableMapOf<String, String>()
        // failPartMessageMap is injected
        var failPartMessageMap = mutableMapOf<String, String>()

        fun parseEvent(payload: Any?): Event? {
            (payload as? OSRFObject)?.let { return parseEvent(it) }
            (payload as? JSONDictionary)?.let { return parseEvent(OSRFObject(it)) }
            return null
        }

        /**
         * return an Event if [obj] is an Evergreen "event" (error) or null if not.
         * See also AppUtils::is_event and Account::attempt_hold_placement.
         */
        fun parseEvent(obj: OSRFObject): Event? {
            // case 1: obj is an event
            val ilsevent = obj.get("ilsevent")
            val textcode = obj.getString("textcode")
            val desc = obj.getString("desc")
            if (ilsevent != null && textcode != null && textcode != "SUCCESS" && desc != null) {
                return Event(obj)
            }

            // case 2: obj has a last_event, or a result with a last_event
            val lastEvent = obj.getObject("result")?.getObject("last_event")
                    ?: obj.getObject("last_event")
            if (lastEvent != null) {
                return parseEvent(lastEvent)
            }

            // case 3: obj has a result that is an array of events
            val objList = obj.get("result") as? ArrayList<OSRFObject>
            val firstObj = objList?.firstOrNull()
            if (firstObj != null) {
                return parseEvent(firstObj)
            }

            return null
        }
    }
}
