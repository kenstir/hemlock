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

package org.evergreen_ils.gateway

import net.kenstir.data.jsonMapOf
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

    // Test to understand warning: Unchecked cast of Any? to List<OSRFObject>
    @Test
    fun test_uncheckedCast_notAList() {
        val obj = OSRFObject(jsonMapOf("settings" to "0"))
        val settings = obj.get("settings") as? List<OSRFObject>
        assertNull(settings)
        // As expected; as? returns null
    }

    // Test to understand warning: Unchecked cast of Any? to List<OSRFObject>
    @Test
    fun test_uncheckedCast_listContainsNonObjects() {
        val obj = OSRFObject(jsonMapOf("settings" to arrayListOf(1, 2, 3)))

        // as? returns non-null
        val settings = obj.get("settings") as? List<OSRFObject>
        assertNotNull(settings)

        // it's a list even
        assertEquals(3, settings?.size)

        // but now comes the error; first() is not an OSRFObject and fails with ClassCastException
        val res = kotlin.runCatching { settings?.first() }
        assertTrue(res.isFailure)
        val err = res.exceptionOrNull()
        println("Exception: $err")
        assertTrue(err is ClassCastException)
    }

    @Test
    fun test_getObjectList_keyMissing() {
        val obj = OSRFObject(jsonMapOf())
        val settings = obj.getObjectList("settings")
        assertNull(settings)
    }

    @Test
    fun test_getObjectList_notAList() {
        val obj = OSRFObject(jsonMapOf("settings" to "0"))
        val res = kotlin.runCatching { obj.getObjectList("settings") }
        assertTrue(res.isFailure)
        val err = res.exceptionOrNull()
        assertTrue(err is IllegalArgumentException)
        assertEquals("obj[settings] is not a list: 0", err?.message)
    }

    @Test
    fun test_getObjectList_listContainsNonObjects() {
        val obj = OSRFObject(jsonMapOf("settings" to arrayListOf(1, 2, 3)))
        val res = kotlin.runCatching { obj.getObjectList("settings") }
        assertTrue(res.isFailure)
        res.exceptionOrNull()?.message?.let { msg ->
            println(msg)
            assertTrue(msg.contains("obj[settings] is not an object list:"))
        }
    }

    @Test
    fun test_getObjectList_emptyList() {
        val obj = OSRFObject(
            jsonMapOf(
                "settings" to arrayListOf<OSRFObject>()
            )
        )
        val settings = obj.getObjectList("settings")
        assertEquals(0, settings?.size)
    }

    @Test
    fun test_getObjectList_ok() {
        val obj = OSRFObject(
            jsonMapOf(
                "settings" to arrayListOf(
                    OSRFObject(jsonMapOf("id" to 1)),
                    OSRFObject(jsonMapOf("id" to 2))
                )
            )
        )
        val settings = obj.getObjectList("settings")
        assertEquals(2, settings?.size)
    }
}
