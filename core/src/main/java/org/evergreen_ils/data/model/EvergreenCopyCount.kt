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

import net.kenstir.data.model.CopyCount
import org.evergreen_ils.gateway.OSRFObject

class EvergreenCopyCount(obj: OSRFObject): CopyCount {
    override val orgId: Int = obj.getInt("org_unit") ?: 1
    override val count: Int = obj.getInt("count") ?: 0
    override val available: Int = obj.getInt("available") ?: 0

    companion object {
        fun makeArray(l: List<OSRFObject>): ArrayList<CopyCount> {
            val ret = ArrayList<CopyCount>()
            for (obj in l) {
                val info = EvergreenCopyCount(obj)
                ret.add(info)
            }
            return ret
        }
    }
}
