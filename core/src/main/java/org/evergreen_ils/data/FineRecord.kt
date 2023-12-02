/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
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
package org.evergreen_ils.data

import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.android.Log
import org.opensrf.util.OSRFObject
import java.util.*

private val TAG = FineRecord::class.java.simpleName

class FineRecord(circ: OSRFObject?, val mvrObj: OSRFObject?, mbtsObj: OSRFObject) {
    var title: String? = null
    var subtitle: String? = null
    var balanceOwed: Double? = null
    var maxFine: Double? = null
    private var checkinTime: Date? = null

    val status: String
        get() {
            if (mvrObj == null) return ""
            if (checkinTime != null) return "returned"
            if (balanceOwed != null && maxFine != null && balanceOwed!! >= maxFine!!) return "maximum fine"
            return "fines accruing"
        }

    init {
        if (mbtsObj["xact_type"].toString() == "circulation") {
            title = mvrObj?.getString("title")
            subtitle = mvrObj?.getString("author")
            checkinTime = OSRFUtils.parseDate(circ?.getString("checkin_time"))
        } else { // xact_type = "grocery"
            title = mbtsObj.getString("last_billing_type")
            subtitle = mbtsObj.getString("last_billing_note")
        }
        try {
            balanceOwed = mbtsObj.getString("balance_owed")?.toDouble()
        } catch (e: NumberFormatException) {
            Log.d(TAG, "error converting double", e)
        }
        try {
            maxFine = circ?.getString("max_fine")?.toDouble()
        } catch (e: NumberFormatException) {
            Log.d(TAG, "error converting double", e)
        }
    }

    companion object {
        @JvmStatic
        fun makeArray(payload: List<Any>): List<FineRecord> {
            val ret = mutableListOf<FineRecord>()
            val records = payload as? List<JSONDictionary>
            if (records != null) {
                for (item in records) {
                    val mbts = item["transaction"] as? OSRFObject ?: continue
                    val circ = item["circ"] as? OSRFObject
                    val mvr = item["record"] as? OSRFObject
                    val record = FineRecord(circ, mvr, mbts)
                    ret.add(record)
                }
            }
            return ret
        }
    }
}
