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

import net.kenstir.data.model.OrgClosure
import net.kenstir.data.model.OrgClosureInfo
import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.gateway.OSRFObject
import java.util.Date

class EvergreenOrgClosure(
    start: Date,
    end: Date,
    reason: String,
    val obj: OSRFObject
): OrgClosure(start, end, reason) {
    override fun toInfo(): OrgClosureInfo {
        val startDateString = OSRFUtils.formatDateForOutput(start)
        val isFullDay = obj.getBoolean("full_day")
        val isMultiDay = obj.getBoolean("multi_day")
        var isDateRange = false
        val dateString = when {
            isMultiDay -> {
                isDateRange = true
                val endDateString = OSRFUtils.formatDateForOutput(end)
                "$startDateString - $endDateString"
            }
            isFullDay -> {
                startDateString
            }
            else -> {
                isDateRange = true
                val startDateTimeString = OSRFUtils.formatDateTimeForOutput(start)
                val endDateTimeString = OSRFUtils.formatDateTimeForOutput(end)
                "$startDateTimeString - $endDateTimeString"
            }
        }
        return OrgClosureInfo(
            dateString = dateString,
            reason = reason,
            isDateRange = isDateRange
        )
    }

    companion object {
        fun makeArray(objList: List<OSRFObject>): List<OrgClosure> {
            val now = Date()
            val ret = mutableListOf<OrgClosure>()
            for (obj in objList) {
                val start = obj.getDate("close_start")
                val end = obj.getDate("close_end")
                if (start != null && end != null && end > now) {
                    val reason = obj.getString("reason") ?: "No reason provided"
                    ret.add(EvergreenOrgClosure(start, end, reason, obj))
                }
            }
            return ret
        }
    }
}
