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

package org.evergreen_ils.test

import org.evergreen_ils.Api
import org.evergreen_ils.net.GatewayEventError
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.open_ils.Event
import org.opensrf.util.GatewayResult
import org.opensrf.util.OSRFRegistry

class GatewayResultTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            val fields = arrayOf("juvenile","usrname","home_ou")
            OSRFRegistry.registerObject("au", OSRFRegistry.WireProtocol.ARRAY, fields)
        }
    }

    @Test
    fun test_registeredObj() {
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser",69]}]}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asObject()
        assertNotNull(obj)
        assertEquals(false, Api.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(69, obj.getInt("home_ou"))
    }

    @Test
    fun test_unregisteredClass() {
        val json = """
            {"status":200,"payload":[{"__c":"xyzzy","__p":["f","luser",69]}]}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("Unregistered class: xyzzy", result.errorMessage)

        val res = kotlin.runCatching { result.asObject() }
        assertTrue(res.isFailure)
    }

    @Test
    fun test_classlessObject() {
        val json = """
            {"payload":[{"queue_position":2,"potential_copies":12,"status":7,"total_holds":3,"estimated_wait":0}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asObject()
        assertNotNull(obj)
        if (obj == null) return
        assertEquals(3, obj.getInt("total_holds"))
        assertEquals(2, obj.getInt("queue_position"))
    }

    @Test
    fun test_failureOnBadStatus() {
        val json = """
            {"payload":[],"status":400}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)

        val objResult = kotlin.runCatching { result.asObject() }
        assertTrue(objResult.isFailure)
        val strResult = kotlin.runCatching { result.asString() }
        assertTrue(strResult.isFailure)
        val arrayResult = kotlin.runCatching { result.asObjectArray() }
        assertTrue(arrayResult.isFailure)
    }

    // not sure if this happens IRL
    @Test
    fun test_emptyPayload() {
        val json = """
            {"payload":[],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asOptionalObject()
        assertNull(obj)

        val objResult = kotlin.runCatching { result.asObject() }
        assertTrue(objResult.isFailure)
        val strResult = kotlin.runCatching { result.asString() }
        assertTrue(strResult.isFailure)
        val arrayResult = kotlin.runCatching { result.asObjectArray() }
        assertTrue(arrayResult.isFailure)
    }

    @Test
    fun test_string() {
        val json = """
            {"payload":["3-2-8"],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val str = result.asString()
        assertEquals("3-2-8", str)
    }

    @Test
    fun test_emptyArray() {
        // open-ils.actor.user.transactions.have_charge.fleshed response if no charges
        val json = """
            {"payload":[[]],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val objResult = kotlin.runCatching { result.asObject() }
        assertTrue(objResult.isFailure)
        val strResult = kotlin.runCatching { result.asString() }
        assertTrue(strResult.isFailure)
        val arr = result.asObjectArray()
        assertEquals(0, arr.size)
        val arr2 = result.asArray()
        assertEquals(0, arr2.size)
    }

    @Test
    fun test_authSuccessEvent() {
        // open-ils.auth.authenticate.complete returns this odd looking event.
        // For now we don't use GatewayResult to deal with its contents, but we could.
        val json = """
            {"payload":[{"ilsevent":0,"textcode":"SUCCESS","desc":"Success","pid":6939,"stacktrace":"oils_auth.c:634","payload":{"authtoken":"985cda3d943232fbfd987d85d1f1a8af","authtime":420}}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)
        val map = result.payload as? Map<String, Any?>
        assertNotNull(map)
        assertEquals("SUCCESS", map?.get("textcode"))

        val obj = result.asObject()
        assertEquals(0, obj.getInt("ilsevent"))
    }

    @Test
    fun test_failureWithSingleEvent() {
        val json = """
            {"payload":[{"ilsevent":1001,"textcode":"NO_SESSION","desc":"User login session has either timed out or does not exist","pid":88967,"stacktrace":"oils_auth.c:1150"}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("User login session has either timed out or does not exist", result.errorMessage)

        val res = kotlin.runCatching { result.asObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals(error?.ev?.code, 1001)
        assertEquals(error?.ev?.textCode, "NO_SESSION")
        assertTrue(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_failureWithMultipleEvents() {
        val json = """
            {"payload":[[{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_FINES"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"7013","pid":23476,"textcode":"PATRON_EXCEEDS_FINES","desc":"The patron in question has reached the maximum fine amount"},{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_LOST_COUNT"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"1236","pid":23476,"textcode":"PATRON_EXCEEDS_LOST_COUNT","desc":"The patron has too many lost items."}]],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("The patron in question has reached the maximum fine amount\n\nThe patron has too many lost items.", result.errorMessage)

        val res = kotlin.runCatching { result.asObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals(error?.ev?.code, 7013)
        assertEquals(error?.ev?.textCode, "PATRON_EXCEEDS_FINES")
        assertFalse(error?.isSessionExpired() ?: false)
    }
}
