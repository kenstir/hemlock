/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.data

import org.evergreen_ils.utils.TextUtils
import org.opensrf.util.OSRFObject
import java.text.DateFormat
import java.util.*

class CircRecord(circ: OSRFObject?, circType: CircType, circId: Int) {

    constructor(circId: Int) : this(null, CircType.OUT, circId)

    enum class CircType {
        OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED
    }

    var circId = -1
    var circ: OSRFObject?
    @JvmField
    var mvr: OSRFObject? = null
    @JvmField
    var acp: OSRFObject? = null
    @JvmField
    var record: MBRecord? = null

    // dummy_title is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    val title: String?
        get() {
            if (!TextUtils.isEmpty(record?.title))
                return record?.title
            if (!TextUtils.isEmpty(acp?.getString("dummy_title")))
                return acp?.getString("dummy_title")
            return "Unknown Title"
        }

    // dummy_author is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    val author: String?
        get() {
            if (!TextUtils.isEmpty(record?.author))
                return record?.author
            if (!TextUtils.isEmpty(acp?.getString("dummy_author")))
                return acp?.getString("dummy_author")
            return ""
        }

    val dueDate: Date?
        get() = circ?.getDate("due_date")

    val dueDateString: String
        get() = if (dueDate != null) DateFormat.getDateInstance().format(dueDate) else ""

    val renewals: Int
        get() = circ?.getInt("renewal_remaining") ?: 0

    val autoRenewals: Int
        get() = circ?.getInt("auto_renewal_remaining") ?: 0

    val wasAutorenewed: Boolean
        get() = circ?.getBoolean("auto_renewal") ?: false

    val targetCopy: Int?
        get() = circ?.getInt("target_copy")

    val isOverdue: Boolean
        get() {
            val currentDate = Date()
            return dueDate?.before(currentDate) ?: false
        }

    val isDueSoon: Boolean
        get() {
            if (dueDate == null) return false
            // this is effectively 3 days, because dueDate is at 23:59:59
            val threeDaysPrior = 4
            val cal = Calendar.getInstance()
            cal.time = dueDate
            cal.add(Calendar.DAY_OF_MONTH, -threeDaysPrior)
            val currentDate = Date()
            return currentDate > cal.time
        }

    init {
        this.circ = circ
        this.circId = circId
    }

    companion object {
        fun makeArray(circSlimObj: OSRFObject): ArrayList<CircRecord> {
            val ret = ArrayList<CircRecord>()
            for (id in OSRFUtils.parseIdsListAsInt(circSlimObj.get("out")))
                ret.add(CircRecord(id))
            for (id in OSRFUtils.parseIdsListAsInt(circSlimObj.get("overdue")))
                ret.add(CircRecord(id))
            return ret
        }
    }
}
