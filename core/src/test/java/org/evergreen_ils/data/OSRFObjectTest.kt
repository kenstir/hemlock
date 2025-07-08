/*
 * Copyright (c) 2019 Kenneth H. Cox
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

package org.evergreen_ils.data

import net.kenstir.data.jsonMapOf
import org.evergreen_ils.gateway.OSRFObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class OSRFObjectTest {

    @Test
    fun test_basic() {
        val obj = OSRFObject(
            jsonMapOf(
                "id" to 42,
                "home_ou" to 69,
                "day_phone" to "508-555-1212"
            )
        )
        assertEquals(42, obj.getInt("id"))
        assertEquals("508-555-1212", obj.getString("day_phone"))
        assertNull(obj.getString("na"))
    }

    @Test
    fun test_getBoolean() {
        val obj = OSRFObject(
            jsonMapOf(
            "juvenile" to "f", "active" to "t"
        )
        )
        assertEquals(true, obj.getBoolean("active"))
        assertEquals(false, obj.getBoolean("juvenile"))
    }

    @Test
    fun test_getInt() {
        val obj = OSRFObject(
            jsonMapOf(
            "id" to 42,
            "count" to "42",
            "null_key" to null
        )
        )
        assertEquals(42, obj.getInt("id"))
        assertEquals(42, obj.getInt("count"))
        assertNull(obj.getInt("null_key"))
    }

    @Test
    fun test_getObject() {
        val cardObj = OSRFObject(jsonMapOf("barcode" to "1234"))
        val fleshedUserObj = OSRFObject(jsonMapOf("card" to cardObj))
        val obj = fleshedUserObj.getObject("card")
        assertEquals("1234", obj?.getString("barcode"))
        assertNull(obj?.getObject("na"))
    }

    @Test
    fun test_getDate() {
        val obj = OSRFObject(
            jsonMapOf(
            "reason" to "Good Friday",
            "close_start" to "2024-03-29T12:00:47-0400",
            "close_end" to "2024-03-29T20:00:47-0400",
            "org_unit" to 69,
            "multi_day" to "f",
            "full_day" to "f"
        )
        )
        val start = obj.getDate("close_start")
        assertNotNull(start)
        val end = obj.getDate("close_end")
        assertNotNull(end)
        if (start != null && end != null) {
            val diff = end.time - start.time
            print("start: $start")
            print("end:   $end")
            print("diff:  $diff")
            val diffInHours = TimeUnit.MILLISECONDS.toHours(diff)
            assertEquals(8, diffInHours)
        }
    }

    // Test to understand warning: Unchecked cast: Any! to List<XOSRFObject>
    @Test
    fun test_misc_uncheckedCast1() {
        val obj = OSRFObject(
            jsonMapOf(
            "settings" to "0"
        )
        )
        val settings = obj.get("settings") as? List<OSRFObject>
        assertNull(settings)
        // As Expected; as? returns null
    }

    // Test to understand warning: Unchecked cast: Any! to List<XOSRFObject>
    @Test
    fun test_misc_uncheckedCast2() {
        val obj = OSRFObject(
            jsonMapOf(
            "settings" to arrayListOf(
                OSRFObject(jsonMapOf("id" to 1)),
                OSRFObject(jsonMapOf("id" to 2))
            )
        )
        )
        val settings = obj.get("settings") as? List<OSRFObject>
        assertNotNull(settings)
        assertEquals(2, settings?.size)
        assertTrue(settings?.first() is OSRFObject)
        // As Expected; as? returns non-null
    }

    // Test to understand warning: Unchecked cast: Any! to List<XOSRFObject>
    @Test
    fun test_misc_uncheckedCast3() {
        val obj = OSRFObject(
            jsonMapOf(
            "settings" to arrayListOf(1,2,3)
        )
        )
        val settings = obj.get("settings") as? List<OSRFObject>
        assertNotNull(settings)
//        val first = settings?.first()
//        assertEquals(1, settings?.first())
        // Surprising: as? returns non-null but the result is a List<Int>,
        // first() fails with a ClassCastException.
    }

    // Try to avoid unchecked cast using filterIsInstance
    @Test
    fun test_misc_uncheckedCast4() {
        val obj = OSRFObject(
            jsonMapOf(
            "settings" to arrayListOf(1,2,3)
        )
        )
        val settings = obj.get("settings") as? List<*>
        assertNotNull(settings)
        val l = settings?.filterIsInstance<OSRFObject>()
        assertEquals(0, l?.size)
        // As Expected; as? returns List, filterIsInstance returns empty.
    }
}
