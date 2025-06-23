/*
 * Copyright (c) 2020 Kenneth H. Cox
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

package net.kenstir.apps.core

import org.evergreen_ils.data.OSRFUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.opensrf.util.OSRFObject

class ApiTest {

    var sessionObj = OSRFObject(mapOf<String, Any?>(
            "id" to 42,
            "home_ou" to 69,
            "day_phone" to "508-555-1212"
    ))
    var cardObj = OSRFObject(mapOf<String, Any?>("barcode" to "1234"))

    @Before
    fun setUp() {
    }

    @Test
    fun test_parseBoolean() {
        Assert.assertEquals(true, OSRFUtils.parseBoolean("t"))
        Assert.assertEquals(true, OSRFUtils.parseBoolean(true))

        Assert.assertEquals(false, OSRFUtils.parseBoolean("f"))
        Assert.assertEquals(false, OSRFUtils.parseBoolean(false))

        // anything else is false
        Assert.assertEquals(false, OSRFUtils.parseBoolean(null))
        Assert.assertEquals(false, OSRFUtils.parseBoolean("jibberish"))
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
