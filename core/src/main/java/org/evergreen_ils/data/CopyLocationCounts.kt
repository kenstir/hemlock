/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
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

import org.evergreen_ils.system.EgCopyStatus

class CopyLocationCounts(val orgId: Int, val callNumberPrefix: String, val callNumberLabel: String, val callNumberSuffix: String, val copyLocation: String) {
    var countsByStatus = mutableListOf<Pair<Int, Int>>() // (copyStatusId, count)

    val callNumber: String
        get() {
            return listOf(callNumberPrefix, callNumberLabel, callNumberSuffix).joinToString(" ").trim()
        }
    val countsByStatusLabel: String
        get() {
            var arr = mutableListOf<String>()
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
                val countsByStatusMap = a[5] as? Map<String, Int> ?: continue

                val clc = CopyLocationCounts(orgId, callNumberPrefix, callNumberLabel, callNumberSuffix, copyLocation)
                ret.add(clc)
                for ((k, v) in countsByStatusMap) {
                    val id = k.toIntOrNull() ?: continue
                    clc.countsByStatus.add(Pair(id, v))
                }
                clc.countsByStatus.sortBy { it.first }
            }
            return ret
        }
    }
}
