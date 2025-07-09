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
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRecordTest {
    @Test
    fun test_noBibRecord() {
        val auchObj = OSRFObject(
            mapOf(
                "id" to 10178625,
                "target_copy" to 409071,
                "returned_date" to "2025-01-22T17:31:11-0500"
            )
        )
        val historyRecord = EvergreenHistoryRecord(10178625, auchObj)

        assertEquals("Unknown Title", historyRecord.title)
        assertEquals("", historyRecord.author)
        assertEquals(409071, historyRecord.targetCopy)
    }

    @Test
    fun test_withRecord() {
        val auchObj = OSRFObject(
            mapOf(
                "id" to 10178625,
                "target_copy" to 409071,
                "returned_date" to "2025-01-22T17:31:11-0500"
            )
        )
        val historyRecord = EvergreenHistoryRecord(10178625, auchObj)
        historyRecord.record = MBRecord(12345, OSRFObject(jsonMapOf(
            "title" to "Record Title",
            "author" to "Record Author",
        )))

        assertEquals("Record Title", historyRecord.title)
        assertEquals("Record Author", historyRecord.author)
        assertEquals(409071, historyRecord.targetCopy)
    }
}
