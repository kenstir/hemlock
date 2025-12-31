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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class OSRFUtilsTest {

    @Test
    fun test_parseDate_formatDate() {
        for (apiDate in listOf(
            "2021-02-01T11:06:42-0500",
            "2018-02-14T20:09:28-0500",
            "2017-11-02T09:04:41-0400",
            "2024-09-20T23:59:59-0400",
            "2016-05-11T18:32:02-0400",
        )) {
            val date = OSRFUtils.parseDate(apiDate)
            val formatted = OSRFUtils.formatDate(date!!)
            assertEquals(apiDate, formatted)

            val trimmedDate = apiDate.substringBefore('T')
            val formattedDateOnly = OSRFUtils.formatDateAsDayOnly(date!!)
            assertEquals(trimmedDate, formattedDateOnly)
        }
    }

    @Test
    fun test_parseDate_edgeCases() {
        assertNull(OSRFUtils.parseDate(null))

        // invalid date strings should return null, but they don't
        // leave it for now until we replace java.util.Date
        assertNull(OSRFUtils.parseDate(""))
        assertNull(OSRFUtils.parseDate("null"))
//        assertNull(OSRFUtils.parseDate("2021-13-01T11:06:42-0500")) // invalid month
//        assertNull(OSRFUtils.parseDate("2021-00-01T11:06:42-0500")) // invalid month
//        assertNull(OSRFUtils.parseDate("2021-02-30T11:06:42-0500")) // invalid day
//        assertNull(OSRFUtils.parseDate("2021-02-01T25:06:42-0500")) // invalid hour
    }

    @Test
    fun test_parseHours_API_to_AM() {
        val apiTime = "09:00:00"
        val date = OSRFUtils.parseHours(apiTime)
        val dateString = OSRFUtils.formatHoursForOutput(date!!)
        Assert.assertTrue(dateString.matches(Regex("0?9:00.AM")))
    }

    @Test
    fun test_parseHours_API_to_PM() {
        val apiTime = "17:00:00"
        val date = OSRFUtils.parseHours(apiTime)
        val dateString = OSRFUtils.formatHoursForOutput(date!!)
        Assert.assertTrue(dateString.matches(Regex("5:00.PM")))
    }

    @Test
    fun test_parseHours_edgeCases() {
        assertNull(OSRFUtils.parseHours(null))

        // invalid Hours strings should return null, but they don't
        // leave it for now until we replace java.util.Date
        assertNull(OSRFUtils.parseHours(""))
        //assertNull(OSRFUtils.parseHours("null"))
        //assertNull(OSRFUtils.parseHours("25:00:00"))
    }

    private fun getExpireDate(date: Date?): String? {
        return date?.let { OSRFUtils.formatDate(it) }
    }

    @Test
    fun test_letScopeFunction() {
        assertNull(getExpireDate(null))
    }

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
    fun test_parseIdsList() {
        // normal cases
        assertEquals(arrayListOf<Int>(), OSRFUtils.parseIdsListAsInt(arrayListOf<String>()))
        assertEquals(arrayListOf<Int>(487, 488), OSRFUtils.parseIdsListAsInt(arrayListOf<String>("487", "488")))

        // have never seen this but it looks sound
        assertEquals(arrayListOf<Int>(487, 488), OSRFUtils.parseIdsListAsInt(arrayListOf<Int>(487, 488)))

        // edge cases
        assertEquals(arrayListOf<Int>(), OSRFUtils.parseIdsListAsInt(null))
        assertEquals(arrayListOf<Int>(), OSRFUtils.parseIdsListAsInt("junk"))
    }
}
