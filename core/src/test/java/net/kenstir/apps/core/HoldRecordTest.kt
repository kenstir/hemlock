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

import org.evergreen_ils.accountAccess.holds.HoldRecord
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.opensrf.util.OSRFObject

class HoldRecordTest {
    @Test
    fun test_makeArray() {
        val ahrObj = OSRFObject(mapOf<String, Any?>(
                "id" to 14154079,
                "email_notify" to "t",
                "expire_time" to null,
                "frozen" to "t",
                "hold_type" to "T",
                "phone_notify" to null,
                "pickup_lib" to 69,
                "shelf_expire_time" to null,
                "sms_notify" to "5085551212",
                "target" to "4190606",
                "thaw_date" to null,
                "transit" to null
        ))
        val holdRecords = HoldRecord.makeArray(listOf(ahrObj))
        assertEquals(1, holdRecords.size)
        val hold = holdRecords.firstOrNull()
        assertNotNull(hold)
        if (hold == null) return

        // direct assertions on ahr object
        assertNull(hold.phone_notify)
        assertNull(hold.shelf_expire_time)
        assertNull(hold.thaw_date)
        assertTrue(hold.email_notify)
        assertTrue(hold.suspended)
        assertEquals(69, hold.pickup_lib)
        assertEquals(4190606, hold.target)
        assertEquals("5085551212", hold.sms_notify)

        // assertions on record / hold queue
        assertEquals("Unknown title", hold.title)
        assertEquals("", hold.author)
        assertNull(hold.part_label)
        assertNull(hold.status)
    }

    @Ignore("TODO: implement when I have more data")
    @Test
    fun test_inTransit() {
        val transitObj = OSRFObject(mapOf<String, Any?>(
                "id" to 27468839,
                "source" to 154,
                "dest" to 69,
                "source_send_time" to "2020-01-02T09:54:39-0500"
        ))
        val ahrObj = OSRFObject(mapOf<String, Any?>(
                "id" to 15368911,
                "email_notify" to "t",
                "frozen" to "f",
                "hold_type" to "T",
                "phone_notify" to "5085551212",
                "pickup_lib" to 69,
                "sms_notify" to "5085551212",
                "target" to 2722036,
                "transit" to transitObj
        ))
        val hold = HoldRecord(ahrObj)

        // !!! there are no methods to get transit stats w/o Resources
    }

    @Ignore("TODO: implement when I have more data")
    @Test
    fun test_waitingForCopy() {
        val qstatsObj = OSRFObject(mapOf<String, Any?>(
                "estimated_wait" to null,
                "potential_copies" to 2,
                "queue_position" to 1,
                "status" to 2,
                "total_holds" to 3
        ))
        val ahrObj = OSRFObject(mapOf<String, Any?>(
                "id" to 15368911,
                "email_notify" to "t",
                "expire_time" to null,
                "frozen" to "f",
                "hold_type" to "T",
                "phone_notify" to "5085551212",
                "pickup_lib" to 69,
                "shelf_expire_time" to null,
                "sms_notify" to "5085551212",
                "target" to 2722036
        ))
        val hold = HoldRecord(ahrObj)
        hold.setQueueStats(qstatsObj)
    }
}
