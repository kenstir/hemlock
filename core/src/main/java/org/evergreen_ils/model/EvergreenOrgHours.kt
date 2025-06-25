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

package org.evergreen_ils.model

import net.kenstir.hemlock.data.model.OrgHours
import org.evergreen_ils.data.OSRFUtils
import org.evergreen_ils.xdata.XOSRFObject

class EvergreenOrgHours(
    day0Hours: String?,
    day1Hours: String?,
    day2Hours: String?,
    day3Hours: String?,
    day4Hours: String?,
    day5Hours: String?,
    day6Hours: String?,
): OrgHours(day0Hours, day1Hours, day2Hours, day3Hours, day4Hours, day5Hours, day6Hours) {
    companion object {
        // TODO: [Add Hours of Operation Note field](https://evergreen-ils.org/documentation/release/RELEASE_NOTES_3_10.html#_hours_of_operation_note_field)
        // Look for fields e.g. "dow_0_note"
        fun make(obj: XOSRFObject?): EvergreenOrgHours {
            val day0Hours = hoursOfOperation(obj, 0)
            val day1Hours = hoursOfOperation(obj, 1)
            val day2Hours = hoursOfOperation(obj, 2)
            val day3Hours = hoursOfOperation(obj, 3)
            val day4Hours = hoursOfOperation(obj, 4)
            val day5Hours = hoursOfOperation(obj, 5)
            val day6Hours = hoursOfOperation(obj, 6)
            return EvergreenOrgHours(
                day0Hours,
                day1Hours,
                day2Hours,
                day3Hours,
                day4Hours,
                day5Hours,
                day6Hours
            )
        }

        private fun hoursOfOperation(obj: XOSRFObject?, day: Int): String? {
            val openTimeApi = obj?.getString("dow_${day}_open")
            val closeTimeApi = obj?.getString("dow_${day}_close")
            val openTime = OSRFUtils.parseHours(openTimeApi)
            val closeTime = OSRFUtils.parseHours(closeTimeApi)
            if (openTime == null || closeTime == null) {
                return null
            }
            if (openTimeApi == closeTimeApi) {
                return "closed"
            }
            val openTimeLocal = OSRFUtils.formatHoursForOutput(openTime)
            val closeTimeLocal = OSRFUtils.formatHoursForOutput(closeTime)
            return "$openTimeLocal - $closeTimeLocal"
        }
    }
}
