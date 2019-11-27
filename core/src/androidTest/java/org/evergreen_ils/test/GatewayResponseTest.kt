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
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.GatewayResponse
import org.opensrf.util.JSONException
import org.opensrf.util.OSRFObject
import org.opensrf.util.OSRFRegistry

class GatewayResponseTest {

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
        val response = GatewayResponse.create(json)
        assertFalse(response.failed)
        val obj = response.asObject()
        assertNotNull(obj)
        assertEquals(false, Api.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(69, obj.getInt("home_ou"))
    }

    @Test
    fun test_unregisteredObj() {
        val json = """
            {"status":200,"payload":[{"__c":"xyzzy","__p":["f","luser",69]}]}
            """
        val response = GatewayResponse.create(json)
        assertTrue(response.failed)
        assertTrue(response.ex is JSONException)
    }

    @Test
    fun test_failedOnBadStatus() {
        val json = """
            {"payload":[],"status":400}
            """
        val response = GatewayResponse.create(json)
        assertTrue(response.failed)
    }

    @Test
    fun test_string() {
        val json = """
            {"payload":["3-2-8"],"status":200}
            """
        val response = GatewayResponse.create(json)
        assertFalse(response.failed)
        val str = response.asString()
        assertEquals("3-2-8", str)
    }

    @Test
    fun test_authSuccess() {
        val json = """
            {"payload":[{"ilsevent":0,"textcode":"SUCCESS","desc":"Success","pid":6939,"stacktrace":"oils_auth.c:634","payload":{"authtoken":"985cda3d943232fbfd987d85d1f1a8af","authtime":420}}],"status":200}
            """
        val response = GatewayResponse.create(json)
        assertFalse(response.failed)
        val obj = response.asObject()
        assertEquals("SUCCESS", obj.getString("textcode"))
    }

    @Test
    fun test_failureWithSingleEvent() {
        val json = """
            {"payload":[{"ilsevent":1001,"textcode":"NO_SESSION","desc":"User login session has either timed out or does not exist","pid":88967,"stacktrace":"oils_auth.c:1150"}],"status":200}
            """
        val response = GatewayResponse.create(json)
        assertTrue(response.failed)
        assertEquals("User login session has either timed out or does not exist", response.description)
    }

    @Test
    fun test_failureWithMultipleEvents() {
        val json = """
            {"payload":[[{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_FINES"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"7013","pid":23476,"textcode":"PATRON_EXCEEDS_FINES","desc":"The patron in question has reached the maximum fine amount"},{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_LOST_COUNT"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"1236","pid":23476,"textcode":"PATRON_EXCEEDS_LOST_COUNT","desc":"The patron has too many lost items."}]],"status":200}
            """
        val response = GatewayResponse.create(json)
        assertTrue(response.failed)
        assertEquals("The patron in question has reached the maximum fine amount\n\nThe patron has too many lost items.", response.description)
    }

    @Test
    fun test_emptyArray() {
        val json = """
            {"payload":[[]],"status":200}
            """
        val response = GatewayResponse.create(json)
        assertFalse(response.failed)
        val arr = response.asObjectArray()
        assertEquals(0, arr?.size)
    }
}