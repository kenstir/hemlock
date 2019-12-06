/*
 * Copyright (c) 2019 Kenneth H. Cox
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

import org.evergreen_ils.Api
import org.evergreen_ils.system.Log
import org.opensrf.util.OSRFObject

object EgCopyStatus {
    var copyStatusList = mutableListOf<CopyStatus>()

    fun loadCopyStatuses(ccs_list: List<OSRFObject>) {
        synchronized(this) {
            copyStatusList.clear()
            for (ccs_obj in ccs_list) {
                if (Api.parseBoolean(ccs_obj.getString("opac_visible"))) {
                    val id = ccs_obj.getInt("id")
                    val name = ccs_obj.getString("name")
                    if (id != null && name != null) {
                        copyStatusList.add(CopyStatus(id, name))
                        Log.d(org.evergreen_ils.api.TAG, "loadCopyStatuses id:$id name:$name")
                    }
                }
            }
            copyStatusList.sort()
        }
    }
}
