package org.open_ils

import org.evergreen_ils.Api
import org.evergreen_ils.data.JSONDictionary
import java.util.*

class Event : HashMap<String, Any?> {
    constructor() {}
    constructor(map: Map<String, Any?>?) : super(map) {}

    val description: String?
        get() = get("desc") as String?

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
        get() = Api.parseInt(get("ilsevent"), 0)

    fun failed(): Boolean {
        return code != 0
    }

    companion object {
        fun parseEvent(payload: Any?): Event? {
            val obj = payload as? JSONDictionary ?: return null
            parseEvent(obj)?.let {
                return it
            }
            val resultObj = obj.get("result") as? JSONDictionary ?: return null
            val lastEvent = resultObj.get("last_event") as? JSONDictionary ?: return null
            parseEvent(lastEvent)?.let {
                return it
            }
            return null
        }
        fun parseEvent(obj: JSONDictionary): Event? {
            if (obj.containsKey("ilsevent") && obj.containsKey("textcode")) {
                return Event(obj)
            }
            return null
        }
    }
}
