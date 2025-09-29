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

import net.kenstir.data.model.OrgHours
import net.kenstir.logging.Log
import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.gateway.OSRFObject

class EvergreenOrgHours(val obj: OSRFObject?) : OrgHours() {
    override val day0Hours: String? = obj?.let { hoursOfOperation(it, 0) }
    override val day1Hours: String? = obj?.let { hoursOfOperation(it, 1) }
    override val day2Hours: String? = obj?.let { hoursOfOperation(it, 2) }
    override val day3Hours: String? = obj?.let { hoursOfOperation(it, 3) }
    override val day4Hours: String? = obj?.let { hoursOfOperation(it, 4) }
    override val day5Hours: String? = obj?.let { hoursOfOperation(it, 5) }
    override val day6Hours: String? = obj?.let { hoursOfOperation(it, 6) }
    override val day0Note: String? = obj?.getString("dow_0_note")
    override val day1Note: String? = obj?.getString("dow_1_note")
    override val day2Note: String? = obj?.getString("dow_2_note")
    override val day3Note: String? = obj?.getString("dow_3_note")
    override val day4Note: String? = obj?.getString("dow_4_note")
    override val day5Note: String? = obj?.getString("dow_5_note")
    override val day6Note: String? = obj?.getString("dow_6_note")

    companion object {
        private fun hoursOfOperation(obj: OSRFObject?, day: Int): String? {
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
