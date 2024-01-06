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

import org.opensrf.util.OSRFObject
import java.util.ArrayList

data class HistoryRecord(val id: Int, val obj: OSRFObject) : java.io.Serializable {

    val createDate = obj.getDate("create_date")
    val readDate = obj.getDate("read_date")
    val isRead = (obj.getDate("read_date") != null)
    val isDeleted = obj.getBoolean("deleted")
    val isPatronVisible = obj.getBoolean("pub")
    val title = obj.getString("title") ?: ""
    val message = obj.getString("message") ?: ""

    companion object {
        fun makeArray(messageList: List<OSRFObject>): ArrayList<PatronMessage> {
            val ret = ArrayList<PatronMessage>()
            messageList.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    ret.add(PatronMessage(id, obj))
                }
            }
            return ret
        }
    }
}
