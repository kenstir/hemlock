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
import org.junit.Assert.*
import org.junit.Test

class PatronMessageTest {

    val unreadMessage = XOSRFObject(
        jsonMapOf(
        "id" to 28295855,
        "deleted" to "f",
        "pub" to "t",
        "title" to "unread test message",
        "message" to "you never picked up your hold so we cancelled it.  bummer dude",
        "create_date" to "2022-04-19T10:03:00-0400",
        "read_date" to null
    )
    )
    val readMessage = XOSRFObject(
        jsonMapOf(
        "id" to 28229453,
        "deleted" to "f",
        "pub" to "t",
        "title" to "read test message",
        "message" to "patron-visible note example [TM]",
        "create_date" to "2022-04-11T14:31:49-0400",
        "read_date" to "2022-05-28T12:17:47-0400"
    )
    )
    val deletedMessage = XOSRFObject(
        jsonMapOf(
        "id" to 11777013,
        "deleted" to "t",
        "pub" to "t",
        "title" to "Hold Available for Pickup",
        "message" to "please pickup your hold dude",
        "create_date" to "2020-08-18T13:46:32-0400",
        "read_date" to "2020-08-19T11:00:00-0400"
    )
    )
    val hiddenMessage = XOSRFObject(
        jsonMapOf(
        "id" to 20249966,
        "deleted" to "t",
        "pub" to "f",
        "title" to "This should not be patron visible",
        "message" to "can u dig it yes I can",
        "create_date" to "2017-03-29T10:07:59-0400",
        "read_date" to null
    )
    )

    @Test
    fun test_makeArray() {
        val msgs = EvergreenPatronMessage.makeArray(arrayListOf(unreadMessage, readMessage, deletedMessage, hiddenMessage))
        assertEquals(4, msgs.size)

        var msg = msgs[0]
        assertEquals("unread test message", msg.title)
        assertFalse(msg.isRead)

        msg = msgs[1]
        assertEquals("read test message", msg.title)
        assertTrue(msg.isRead)
    }
}
