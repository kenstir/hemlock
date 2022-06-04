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

private const val TAG = "FineRecord"

class FineRecord(circ: OSRFObject?, mvr_record: OSRFObject?, mbts_transaction: OSRFObject) {
    var record: MBRecord? = null
    var title: String? = null
    var subtitle: String? = null
    var balance_owed: Double? = null
    var max_fine: Double? = null
    private var checkin_time: Date? = null

    val status: String
        get() {
            if (record == null) return ""
            if (checkin_time != null) return "returned"
            return if (balance_owed != null && max_fine != null && balance_owed!! >= max_fine!!) "maximum fine" else "fines accruing"
        }

    init {
        if (mbts_transaction["xact_type"].toString() == "circulation") {
            title = mvr_record?.getString("title")
            subtitle = mvr_record?.getString("author")
            checkin_time = OSRFUtils.parseDate(circ?.getString("checkin_time"))
            record = MBRecord(mvr_record)
        } else { // xact_type = "grocery"
            title = mbts_transaction.getString("last_billing_type")
            subtitle = mbts_transaction.getString("last_billing_note")
        }
        try {
            balance_owed = mbts_transaction.getString("balance_owed")?.toDouble()
        } catch (e: NumberFormatException) {
            Log.d(TAG, "error converting double", e)
        }
        try {
            max_fine = circ?.getString("max_fine")?.toDouble()
        } catch (e: NumberFormatException) {
            Log.d(TAG, "error converting double", e)
        }
    }

    companion object {
        @JvmStatic
        fun makeArray(payload: List<Any>): List<FineRecord> {
            var ret = mutableListOf<FineRecord>()
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
