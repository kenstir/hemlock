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

import org.evergreen_ils.Api
import org.evergreen_ils.data.CircRecord
import org.evergreen_ils.data.jsonMapOf
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.opensrf.util.OSRFObject
import org.opensrf.util.OSRFRegistry

class CircRecordTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    @Ignore("until I fix the code")
    @Test
    fun test_noRecordInfo() {
        val circObj = OSRFObject(jsonMapOf(
                "renewal_remaining" to 0,
                "auto_renewal" to "f",
                "auto_renewal_remaining" to 0,
                "id" to 93108558,
                "circ_type" to null,
                "copy_location" to 2356,
                "target_copy" to 19314463,
                "due_date" to "2020-02-05T23:59:59-0500"
        ))
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 93108558)

        //assertEquals("Unknown Title", circRecord.title)
        assertEquals("", circRecord.author)
        assertEquals(0, circRecord.renewals)
        assertEquals(19314463, circRecord.targetCopy)
        assertEquals(Api.parseDate("2020-02-05T23:59:59-0500"), circRecord.dueDate)
    }

    @Test
    fun test_basic() {
        val circObj = OSRFObject(jsonMapOf(
                "renewal_remaining" to 0,
                "auto_renewal" to "f",
                "auto_renewal_remaining" to 0,
                "id" to 93108558,
                "circ_type" to null,
                "copy_location" to 2356,
                "target_copy" to 19314463,
                "due_date" to "2020-02-05T23:59:59-0500"
        ))
        val mvrObj = OSRFObject(jsonMapOf(
                "title" to "The Testaments",
                "author" to "Margaret Atwood"
        ))
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 93108558)
        circRecord.mvr = mvrObj
        circRecord.recordInfo = RecordInfo(mvrObj)

        assertEquals("The Testaments", circRecord.title)
        assertEquals("Margaret Atwood", circRecord.author)
        assertEquals(0, circRecord.renewals)
        assertEquals(19314463, circRecord.targetCopy)
        assertEquals(Api.parseDate("2020-02-05T23:59:59-0500"), circRecord.dueDate)
    }
}
