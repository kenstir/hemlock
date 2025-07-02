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


package org.evergreen_ils.data

import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.xdata.XOSRFObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CircRecordTest {

    @Test
    fun test_noRecordInfo() {
        val circObj = XOSRFObject(
            jsonMapOf(
                "renewal_remaining" to 0,
                "auto_renewal" to "f",
                "auto_renewal_remaining" to 0,
                "id" to 93108558,
                "circ_type" to null,
                "copy_location" to 2356,
                "target_copy" to 19314463,
                "due_date" to "2020-02-05T23:59:59-0500"
        )
        )
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 93108558)

        assertEquals("Unknown Title", circRecord.title)
        assertEquals("", circRecord.author)
        assertEquals(0, circRecord.renewals)
        assertEquals(19314463, circRecord.targetCopy)
        assertEquals(OSRFUtils.parseDate("2020-02-05T23:59:59-0500"), circRecord.dueDate)
    }

    @Test
    fun test_basic() {
        val circObj = XOSRFObject(
            jsonMapOf(
                "renewal_remaining" to 0,
                "auto_renewal" to "f",
                "auto_renewal_remaining" to 0,
                "id" to 93108558,
                "circ_type" to null,
                "copy_location" to 2356,
                "target_copy" to 19314463,
                "due_date" to "2020-02-05T23:59:59-0500"
        )
        )
        val mvrObj = XOSRFObject(
            jsonMapOf(
                "doc_id" to 1234,
                "title" to "The Testaments",
                "author" to "Margaret Atwood"
        )
        )
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 93108558)
        circRecord.mvr = mvrObj
        circRecord.record = MBRecord(mvrObj)

        assertEquals("The Testaments", circRecord.title)
        assertEquals("Margaret Atwood", circRecord.author)
        assertEquals(0, circRecord.renewals)
        assertEquals(19314463, circRecord.targetCopy)
        assertEquals(OSRFUtils.parseDate("2020-02-05T23:59:59-0500"), circRecord.dueDate)
    }

    // Something borrowed from another consortium will have a target_copy but
    // a record.id==-1, and the acp will have dummy_title and dummy_author
    @Test
    fun test_illCheckout() {
        val circObj = XOSRFObject(
            jsonMapOf(
                "renewal_remaining" to 0,
                "id" to 1,
                "target_copy" to 1507492,
                "due_date" to "2020-02-05T23:59:59-0500"
        )
        )
        val mvrObj = XOSRFObject(
            jsonMapOf(
                "doc_id" to -1,
                "title" to null,
                "author" to null
        )
        )
        val acpObj = XOSRFObject(
            jsonMapOf(
                "id" to 1507492,
                "dummy_author" to "NO AUTHOR",
                "barcode" to "SEOTESTBARCODE",
                "call_number" to -1,
                "copy_number" to null,
                "dummy_isbn" to "NO ISBN",
                "dummy_title" to "SEO TEST",
                "status" to 1
        )
        )
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 1)
        circRecord.mvr = mvrObj
        circRecord.record = MBRecord(mvrObj)
        circRecord.acp = acpObj

        assertEquals("SEO TEST", circRecord.title)
        assertEquals("NO AUTHOR", circRecord.author)
        assertEquals(1507492, circRecord.targetCopy)
    }

    @Test
    fun test_noDueDate() {
        // seen in the wild via Crashlytics
        val circObj = XOSRFObject(
            jsonMapOf(
                "renewal_remaining" to 0,
                "id" to 1,
                "target_copy" to 1507492,
                //"due_date" to "2020-02-05T23:59:59-0500"
        )
        )
        val circRecord = CircRecord(circObj, CircRecord.CircType.OUT, 1)

        assertEquals(false, circRecord.isDueSoon)
        assertEquals(false, circRecord.isOverdue)
    }

    @Test
    fun test_makeArray() {
        val circSlimObj = XOSRFObject(
            jsonMapOf(
                "long_overdue" to arrayListOf<Any?>(),
                "overdue" to arrayListOf<Any?>(),
                "lost" to arrayListOf<Any?>(),
                "out" to arrayListOf<Any?>("93108558"),
                "claims_returned" to arrayListOf<Any?>()
        )
        )
        val checkouts = CircRecord.makeArray(circSlimObj)
        assertEquals(1, checkouts.size)
        assertEquals(93108558, checkouts.first().circId)
    }
}
