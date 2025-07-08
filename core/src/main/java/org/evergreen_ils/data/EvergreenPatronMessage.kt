/*
 * Copyright (c) 2023 Kenneth H. Cox
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

import net.kenstir.hemlock.data.PatronMessage
import org.evergreen_ils.xdata.XOSRFObject
import java.util.ArrayList

data class EvergreenPatronMessage(override val id: Int, val obj: XOSRFObject) : PatronMessage {

    override val title = obj.getString("title") ?: ""
    override val message = obj.getString("message") ?: ""
    override val createDate = obj.getDate("create_date")
    override val isRead = (obj.getDate("read_date") != null)
    override val isDeleted = obj.getBoolean("deleted")
    override val isPatronVisible = obj.getBoolean("pub")

    companion object {
        fun makeArray(messageList: List<XOSRFObject>): ArrayList<EvergreenPatronMessage> {
            val ret = ArrayList<EvergreenPatronMessage>()
            messageList.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    ret.add(EvergreenPatronMessage(id, obj))
                }
            }
            return ret
        }
    }
}
