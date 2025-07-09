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

package org.evergreen_ils.util

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OSRFUtilsTest {

    @Test
    fun test_parseBoolean() {
        assertEquals(true, OSRFUtils.parseBoolean("t"))
        assertEquals(true, OSRFUtils.parseBoolean(true))

        assertEquals(false, OSRFUtils.parseBoolean("f"))
        assertEquals(false, OSRFUtils.parseBoolean(false))

        // anything else is false
        assertEquals(false, OSRFUtils.parseBoolean(null))
        assertEquals(false, OSRFUtils.parseBoolean("jibberish"))
    }

    @Test
    fun test_parseInt() {
        assertNull(OSRFUtils.parseInt(null))

        assertEquals(42, OSRFUtils.parseInt(Integer(42)))
        assertEquals(42, OSRFUtils.parseInt("42"))

        assertNull(OSRFUtils.parseInt(""))
        assertNull(OSRFUtils.parseInt("null"))
        assertEquals(1, OSRFUtils.parseInt("", 1))
    }

    @Test
    fun test_parseTime_API_to_AM() {
        val apiTime = "09:00:00"
        val date = OSRFUtils.parseHours(apiTime)
        val dateString = OSRFUtils.formatHoursForOutput(date!!)
        Assert.assertTrue(dateString.matches(Regex("0?9:00.AM")))
    }

    @Test
    fun test_parseTime_API_to_PM() {
        val apiTime = "17:00:00"
        val date = OSRFUtils.parseHours(apiTime)
        val dateString = OSRFUtils.formatHoursForOutput(date!!)
        Assert.assertTrue(dateString.matches(Regex("5:00.PM")))
    }
}
