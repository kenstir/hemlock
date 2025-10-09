/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
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

package org.evergreen_ils.data.model

import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.ChargeRecord
import net.kenstir.logging.Log
import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.gateway.OSRFObject
import java.util.*

class FineRecord(circ: OSRFObject?, val mvrObj: OSRFObject?, mbtsObj: OSRFObject): ChargeRecord {
    override var title: String? = null
    override var subtitle: String? = null
    override var balanceOwed: Double? = null
    var maxFine: Double? = null
    private var checkinTime: Date? = null
    override var record: BibRecord? = null

    override val status: String
        get() {
            if (mvrObj == null) return ""
            if (checkinTime != null) return "returned"
            if (balanceOwed != null && maxFine != null && balanceOwed!! >= maxFine!!) return "maximum fine"
            return "fines accruing"
        }

    init {
        if (mvrObj != null) {
            record = MBRecord(mvrObj)
        }
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
        private const val TAG = "FineRecord"

        fun makeArray(objects: List<OSRFObject>): List<ChargeRecord> {
            val ret = mutableListOf<ChargeRecord>()
            for (item in objects) {
                val mbts = item["transaction"] as? OSRFObject ?: continue
                val circ = item["circ"] as? OSRFObject
                val mvr = item["record"] as? OSRFObject
                val record = FineRecord(circ, mvr, mbts)
                ret.add(record)
            }
            return ret
        }
    }
}
