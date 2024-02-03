/*
 * Copyright (c) 2024 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.data

import org.evergreen_ils.utils.TextUtils
import org.opensrf.util.OSRFObject
import java.text.DateFormat
import java.util.ArrayList

data class HistoryRecord(val id: Int, val obj: OSRFObject) : java.io.Serializable {

    var record: MBRecord? = null

    val title: String?
        get() {
            if (!TextUtils.isEmpty(record?.title))
                return record?.title
//            if (!TextUtils.isEmpty(acp?.getString("dummy_title")))
//                return acp?.getString("dummy_title")
            return "Unknown Title"
        }
    val author: String?
        get() {
            if (!TextUtils.isEmpty(record?.author))
                return record?.author
//            if (!TextUtils.isEmpty(acp?.getString("dummy_author")))
//                return acp?.getString("dummy_author")
            return ""
        }
    val dueDate = obj.getDate("due_date")
    val dueDateString: String
        get() = if (dueDate != null) DateFormat.getDateInstance().format(dueDate) else ""
    val checkoutDate = obj.getDate("xact_start")
    val checkoutDateString: String
        get() = if (checkoutDate != null) DateFormat.getDateInstance().format(checkoutDate) else ""
    val returnedDate = obj.getDate("checkin_time")
    val returnedDateString: String
        get() = if (returnedDate != null) DateFormat.getDateInstance().format(returnedDate) else "Not Returned"
    val targetCopy = obj.getInt("target_copy")

    companion object {
        fun makeArray(objects: List<OSRFObject>): ArrayList<HistoryRecord> {
            val ret = ArrayList<HistoryRecord>()
            objects.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    ret.add(HistoryRecord(id, obj))
                }
            }
            return ret
        }
    }
}
