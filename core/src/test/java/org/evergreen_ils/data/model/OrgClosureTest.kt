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

import net.kenstir.data.jsonMapOf
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.util.OSRFUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class OrgClosureTest {
    @Test
    fun test_makeArray() {
        val yesterday = Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000)
        val tomorrow = Date(System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000)
        val dayAfter = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)
        val pastClosure = OSRFObject(
            jsonMapOf(
                "close_start" to OSRFUtils.formatDate(yesterday),
                "close_end" to OSRFUtils.formatDate(yesterday),
                "reason" to "Some Holiday",
                "full_day" to true,
                "multi_day" to false,
            )
        )
        val ongoingClosure = OSRFObject(
            jsonMapOf(
                "close_start" to OSRFUtils.formatDate(yesterday),
                "close_end" to OSRFUtils.formatDate(tomorrow),
                "reason" to "Ongoing Holiday",
                "full_day" to true,
                "multi_day" to true,
            )
        )
        val futureClosure = OSRFObject(
            jsonMapOf(
                "close_start" to OSRFUtils.formatDate(tomorrow),
                "close_end" to OSRFUtils.formatDate(tomorrow),
                "reason" to "Some Other Holiday",
                "full_day" to true,
                "multi_day" to false,
            )
        )
        val futureClosureWithNoReason = OSRFObject(
            jsonMapOf(
                "close_start" to OSRFUtils.formatDate(dayAfter),
                "close_end" to OSRFUtils.formatDate(dayAfter),
                "reason" to null,
                "full_day" to true,
                "multi_day" to false
            )
        )
        val closures = EvergreenOrgClosure.makeArray(listOf(pastClosure, ongoingClosure, futureClosure, futureClosureWithNoReason))

        // closures that ended in the past are skipped
        assertEquals(3, closures.size)

        // closures with no reason get a default reason
        val lastClosure = closures.last()
        assertEquals("No reason provided", lastClosure.reason)
    }
}
