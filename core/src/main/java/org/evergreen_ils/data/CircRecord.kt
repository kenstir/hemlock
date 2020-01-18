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
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.data

import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.utils.TextUtils
import org.opensrf.util.OSRFObject
import java.text.DateFormat
import java.util.*

class CircRecord(circ: OSRFObject, circType: CircType, circId: Int) {
    enum class CircType {
        OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED
    }

    private var circId = -1
    var circ: OSRFObject?
    @JvmField
    var mvr: OSRFObject? = null
    @JvmField
    var acp: OSRFObject? = null
    @JvmField
    var recordInfo: RecordInfo? = null
    private val circType: CircType
    var dueDate: Date? = null

    // dummy_title is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    val title: String?
        get() {
            if (!TextUtils.isEmpty(recordInfo!!.title)) return recordInfo!!.title
            var title: String?
            if (mvr != null) {
                title = mvr!!.getString("title")
                if (!TextUtils.isEmpty(title)) return title
            }
            if (acp != null) {
                title = acp!!.getString("dummy_title")
                if (!TextUtils.isEmpty(title)) return title
            }
            return "Unknown Title"
        }

    // dummy_author is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    val author: String?
        get() {
            var author: String?
            if (mvr != null) {
                author = mvr!!.getString("author")
                if (!TextUtils.isEmpty(author)) return author
            }
            if (acp != null) {
                author = acp!!.getString("dummy_author")
                if (!TextUtils.isEmpty(author)) return author
            }
            return ""
        }

    val dueDateString: String
        get() = DateFormat.getDateInstance().format(dueDate)

    val renewals: Int?
        get() = if (circ != null) circ!!.getInt("renewal_remaining") else null

    val targetCopy: Int?
        get() = if (circ != null) circ!!.getInt("target_copy") else null

    val isOverdue: Boolean
        get() {
            val currentDate = Date()
            return dueDate!!.compareTo(currentDate) < 0
        }

    val isDue: Boolean
        get() {
            val currentDate = Date()
            // Because the due dates in C/W MARS at least are 23:59:59, "3 days" here
            // really behaves like 2 days, highlighting if it's due tomorrow or the next day.
            val ITEM_DUE_HIGHLIGHT_DAYS = 3
            val cal = Calendar.getInstance()
            cal.time = dueDate
            cal.add(Calendar.DAY_OF_MONTH, -ITEM_DUE_HIGHLIGHT_DAYS)
            return currentDate.compareTo(cal.time) > 0
        }

    init {
        this.circ = circ
        this.circType = circType
        this.circId = circId
        dueDate = circ.getDate("due_date")
    }
}
