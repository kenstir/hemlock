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
import net.kenstir.data.model.CircRecord
import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.util.TextUtils
import org.evergreen_ils.gateway.OSRFObject
import java.text.DateFormat
import java.util.*

class EvergreenCircRecord(var circ: OSRFObject?, override var circId: Int): CircRecord {

    constructor(circId: Int) : this(null, circId)

    enum class CircType {
        OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED
    }

    var mvr: OSRFObject? = null
    var acp: OSRFObject? = null
    override var record: BibRecord? = null

    // dummy_title is used for pre-cataloged items; in these cases
    // recordInfo.id == mvr.doc_id == -1
    override val title: String?
        get() {
            if (!TextUtils.isEmpty(record?.title))
                return record?.title
            if (!TextUtils.isEmpty(acp?.getString("dummy_title")))
                return acp?.getString("dummy_title")
            return "Unknown Title"
        }

    // dummy_author is used for pre-cataloged items; in these cases
    // recordInfo.id == mvr.doc_id == -1
    override val author: String?
        get() {
            if (!TextUtils.isEmpty(record?.author))
                return record?.author
            if (!TextUtils.isEmpty(acp?.getString("dummy_author")))
                return acp?.getString("dummy_author")
            return ""
        }

    override val dueDate: Date?
        get() = circ?.getDate("due_date")

    override val dueDateLabel: String
        get() = dueDate?.let { DateFormat.getDateInstance().format(it) } ?: ""

    override val renewals: Int
        get() = circ?.getInt("renewal_remaining") ?: 0

    override val autoRenewals: Int
        get() = circ?.getInt("auto_renewal_remaining") ?: 0

    override val wasAutorenewed: Boolean
        get() = circ?.getBoolean("auto_renewal") ?: false

    override val targetCopy: Int?
        get() = circ?.getInt("target_copy")

    override val isOverdue: Boolean
        get() {
            val currentDate = Date()
            return dueDate?.before(currentDate) ?: false
        }

    override val isDueSoon: Boolean
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

    companion object {
        fun makeArray(circSlimObj: OSRFObject): ArrayList<CircRecord> {
            val ret = ArrayList<CircRecord>()
            for (id in OSRFUtils.parseIdsListAsInt(circSlimObj.get("out")))
                ret.add(EvergreenCircRecord(id))
            for (id in OSRFUtils.parseIdsListAsInt(circSlimObj.get("overdue")))
                ret.add(EvergreenCircRecord(id))
            return ret
        }
    }
}
