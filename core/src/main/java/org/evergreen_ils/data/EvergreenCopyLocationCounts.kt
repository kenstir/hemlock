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

package org.evergreen_ils.data

import net.kenstir.hemlock.data.model.CopyLocationCounts
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.xdata.XOSRFObject

class EvergreenCopyLocationCounts(
    override val orgId: Int,
    val callNumberPrefix: String,
    val callNumberLabel: String,
    val callNumberSuffix: String,
    override val copyLocation: String
): CopyLocationCounts {
    var countsByStatus = mutableListOf<Pair<Int, Int>>() // (copyStatusId, count)

    override val callNumber: String
        get() {
            return listOf(callNumberPrefix, callNumberLabel, callNumberSuffix).joinToString(" ").trim()
        }
    override val countsByStatusLabel: String
        get() {
            val arr = mutableListOf<String>()
            for ((copyStatusId, copyCount) in countsByStatus) {
                val copyStatus = EgCopyStatus.label(copyStatusId)
                arr.add("$copyCount $copyStatus")
            }
            return arr.joinToString("\n")
        }

    companion object {
        // The copy_location_counts response is unusual; it is not an OSRFObject
        // in wire protocol, it is a raw payload, an array of arrays
        @JvmStatic
        fun makeArray(payload: List<Any>): List<CopyLocationCounts> {
            var ret = mutableListOf<CopyLocationCounts>()
            for (elem in payload) {
                val a = elem as? List<Any> ?: continue
                if (a.size < 6) continue
                val orgId = (a[0] as? String)?.toIntOrNull() ?: continue
                val callNumberPrefix = a[1] as? String ?: continue
                val callNumberLabel = a[2] as? String ?: continue
                val callNumberSuffix = a[3] as? String ?: continue
                val copyLocation = a[4] as? String ?: continue
                val countsByStatus = a[5] as? XOSRFObject ?: continue

                val clc = EvergreenCopyLocationCounts(orgId, callNumberPrefix, callNumberLabel, callNumberSuffix, copyLocation)
                ret.add(clc)
                for ((k, v) in countsByStatus.map) {
                    val copyStatusId = k.toIntOrNull() ?: continue
                    val count = v as? Int ?: continue
                    clc.countsByStatus.add(Pair(copyStatusId, count))
                }
                clc.countsByStatus.sortBy { it.first }
            }
            return ret
        }
    }
}
