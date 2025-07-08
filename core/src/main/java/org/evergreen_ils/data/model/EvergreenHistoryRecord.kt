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

package org.evergreen_ils.data.model

import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.HistoryRecord
import org.evergreen_ils.gateway.OSRFObject
import java.text.DateFormat
import java.util.ArrayList

data class EvergreenHistoryRecord(override val id: Int, val obj: OSRFObject) : HistoryRecord {

    override var record: BibRecord? = null

    override val title: String
        get() {
            return record?.title?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
        }
    override val author: String
        get() {
            return record?.author?.takeIf { it.isNotEmpty() } ?: ""
        }
    override val dueDate = obj.getDate("due_date")
    override val dueDateLabel: String
        get() = if (dueDate != null) DateFormat.getDateInstance().format(dueDate) else ""
    override val checkoutDate = obj.getDate("xact_start")
    override val checkoutDateLabel: String
        get() = if (checkoutDate != null) DateFormat.getDateInstance().format(checkoutDate) else ""
    override val returnedDate = obj.getDate("checkin_time")
    override val returnedDateLabel: String
        get() = if (returnedDate != null) DateFormat.getDateInstance().format(returnedDate) else "Not Returned"
    override val targetCopy = obj.getInt("target_copy")

    companion object {
        fun makeArray(objects: List<OSRFObject>): ArrayList<EvergreenHistoryRecord> {
            val ret = ArrayList<EvergreenHistoryRecord>()
            objects.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    ret.add(EvergreenHistoryRecord(id, obj))
                }
            }
            return ret
        }
    }
}
