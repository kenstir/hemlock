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

import androidx.test.platform.app.InstrumentationRegistry
import org.evergreen_ils.data.OSRFUtils
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.logging.StdoutLogProvider
import org.evergreen_ils.net.GatewayEventError
import org.evergreen_ils.system.EgMessageMap
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
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

            val testFields = arrayOf("id","name")
            OSRFRegistry.registerObject("test1", OSRFRegistry.WireProtocol.ARRAY, testFields)

            val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
            EgMessageMap.init(resources)
        }
    }

    @Test
    fun test_payloadFirstAsString() {
        val json = """
            {"payload":["3-2-8"],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val str = result.payloadFirstAsString()
        assertEquals("3-2-8", str)
    }

    @Test
    fun test_payloadFirstAsObject_registeredObj() {
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser",69]}]}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals(false, OSRFUtils.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(69, obj.getInt("home_ou"))
    }

    @Test
    fun test_payloadFirstAsObject_plainObj() {
        val json = """
            {"payload":[{"queue_position":2,"potential_copies":12,"status":7,"total_holds":3,"estimated_wait":0}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals(3, obj.getInt("total_holds"))
        assertEquals(2, obj.getInt("queue_position"))
    }

    @Test
    fun test_payloadAsObjectList_registeredObj() {
        // e.g. open-ils.actor.history.circ
        val json = """
            {"payload":[
            {"__c":"test1","__p":[9297488,"s1"]},
            {"__c":"test1","__p":[9118087,"s2"]}
            ],"status":200}
        """.trimIndent()
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val arr = result.payloadAsObjectList()
        assertEquals(2, arr.size)

        val obj = arr.first()
        assertEquals(9297488, obj.getInt("id"))
        assertEquals("s1", obj.getString("name"))
    }

    @Test
    fun test_payloadAsObjectList_empty() {
        // e.g. open-ils.actor.history.circ
        val json = """
            {"payload":[],"status":200}
        """.trimIndent()
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val arr = result.payloadAsObjectList()
        assertEquals(0, arr.size)
    }

    @Test
    fun test_payloadFirstAsObjectList_registerObj() {
        // e.g. open-ils.actor.org_types.retrieve
        val json = """
            {"payload":[[
            {"__c":"test1","__p":[52,"Sprint (PCS)"]},
            {"__c":"test1","__p":[60,"Verizon Wireless"]},
            {"__c":"test1","__p":[1002,"Google Mobile (Fi)"]}
            ]],"status":200}
        """.trimIndent()
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val arr = result.payloadFirstAsObjectList()
        assertEquals(3, arr.size)

        val obj = arr.first()
        assertEquals(52, obj.getInt("id"))
    }

    @Test
    fun test_payloadFirstAsObjectList_plainObj() {
        val json = """
             {"payload":[[{"label":"JAN 2005","record":2224604}]],"status":200}
             """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val objList = result.payloadFirstAsObjectList()
        assertEquals(1, objList.size)

        val first = objList.firstOrNull()
        assertNotNull(first)
        first?.let { obj ->
            assertEquals("JAN 2005", obj.getString("label"))
            assertEquals(2224604, obj.getInt("record"))
        }
    }

    @Test
    fun test_shortObject() {
        // Decode an object where the wire protocol has fewer elements than the class has fields.
        // This happens in the ORG_TYPES_RETRIEVE response, where the last field is omitted.
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser"]}]}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals(false, OSRFUtils.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(null, obj.getInt("home_ou"))
    }

    @Test
    fun test_tooManyFields() {
        // Decode an object where the wire protocol has more elements than the class has fields.
        // This used to happen before we added the `_sk` cache-busting param when fetching the IDL.
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser", 69, "Unexpected"]}]}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals(false, OSRFUtils.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(69, obj.getInt("home_ou"))
    }

    @Test
    fun test_failureOnUnregisteredClass() {
        val json = """
            {"status":200,"payload":[{"__c":"xyzzy","__p":["f","luser",69]}]}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("Unregistered class: xyzzy", result.errorMessage)

        val res = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(res.isFailure)
    }

    @Test
    fun test_failureOnBadStatus() {
        val json = """
            {"payload":[],"status":400}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)

        val errorMessage = "Request failed with status 400"
        val objResult = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(objResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
        val strResult = kotlin.runCatching { result.payloadFirstAsString() }
        assertTrue(strResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
        val arrayResult = kotlin.runCatching { result.payloadFirstAsObjectList() }
        assertTrue(arrayResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
    }

    // This does happens IRL.  AFAIK it means either an empty checkout history,
    // or the gateway is overloaded.
    @Test
    fun test_emptyPayload() {
        val json = """
            {"payload":[],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        // test conditions where an empty payload is OK
        val obj = result.payloadFirstAsOptionalObject()
        assertNull(obj)
        val arr = result.payloadAsObjectList()
        assertEquals(0, arr.size)

        // test conditions where an empty payload is an error
        val objResult = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(objResult.isFailure)
        assertEquals("Internal Server Error: expected object, got EMPTY", objResult.exceptionOrNull()?.message)
        val strResult = kotlin.runCatching { result.payloadFirstAsString() }
        assertTrue(strResult.isFailure)
        assertEquals("Internal Server Error: expected string, got EMPTY", strResult.exceptionOrNull()?.message)
        val arrayResult = kotlin.runCatching { result.payloadFirstAsObjectList() }
        assertTrue(arrayResult.isFailure)
        assertEquals("Internal Server Error: expected array, got EMPTY", arrayResult.exceptionOrNull()?.message)
    }

    @Test
    fun test_emptyArrayofArray() {
        // open-ils.actor.user.transactions.have_charge.fleshed response if no charges
        val json = """
            {"payload":[[]],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        // test where such payload is OK
        val arr = result.payloadFirstAsObjectList()
        assertEquals(0, arr.size)

        // test conditions where such a payload is an error
        val objResult = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(objResult.isFailure)
        val strResult = kotlin.runCatching { result.payloadFirstAsString() }
        assertTrue(strResult.isFailure)
        val arrayResult = result.payloadFirstAsObjectList()
        assertEquals(0, arrayResult.size)
    }

    @Test
    fun test_authSuccessEvent() {
        // open-ils.auth.authenticate.complete returns this odd looking event.
        // For now we don't use GatewayResult to deal with its contents, but we could.
        val json = """
            {"payload":[{"ilsevent":0,"textcode":"SUCCESS","desc":"Success","payload":{"authtoken":"985cda3d943232fbfd987d85d1f1a8af","authtime":420}}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertNotNull(obj)
        assertEquals("SUCCESS", obj.get("textcode"))
        assertEquals(0, obj.getInt("ilsevent"))
    }

    @Test
    fun test_failureWithSingleEvent() {
        val json = """
            {"payload":[{"ilsevent":1001,"textcode":"NO_SESSION","desc":"User login session has either timed out or does not exist"}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("User login session has either timed out or does not exist", result.errorMessage)

        val res = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals(error?.ev?.textCode, "NO_SESSION")
        assertEquals(error?.ev?.failPart, null)
        assertTrue(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_failureWithEventList() {
        // This payload is an array of events
        val json = """
            {"payload":[[{"payload":{"fail_part":"PATRON_EXCEEDS_FINES"},"ilsevent":"7013","textcode":"PATRON_EXCEEDS_FINES","desc":"The patron in question has reached the maximum fine amount"},{"payload":{"fail_part":"PATRON_EXCEEDS_LOST_COUNT"},"ilsevent":"1236","textcode":"PATRON_EXCEEDS_LOST_COUNT","desc":"The patron has too many lost items."}]],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        val customMessage = "Patron has reached the maximum fine amount" // event_msg_map.json
        assertEquals(customMessage, result.errorMessage)

        val res = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals("PATRON_EXCEEDS_FINES", error?.ev?.textCode)
        assertEquals(customMessage, error?.ev?.message)
        assertFalse(error?.isSessionExpired() ?: false)
    }

    /** Does not test anything new, but is a helpful reference for a successful hold */
    @Test
    fun test_placeHold_success() {
        val json = """
            {"payload":[{"result":6309896,"target":21296176}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.payloadFirstAsObject()
        assertEquals(obj.getInt("result"), 6309896)
    }

    @Test
    fun test_placeHold_failWithFailPart() {
        val json = """
            {"payload":[{"result":{"success":0,"last_event":{"payload":{"fail_part":"config.hold_matrix_test.holdable"},"textcode":"ITEM_NOT_HOLDABLE","ilsevent":"1220","desc":"A copy with a remote circulating library (circ_lib) was encountered"},"age_protected_copy":0,"place_unfillable":0},"target":6249829}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        val customMessage = "Hold rules reject this item as unholdable" // fail_part_msg_map.json
        assertEquals(customMessage, result.errorMessage)

        val res = kotlin.runCatching { result.payloadFirstAsObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals("ITEM_NOT_HOLDABLE", error?.ev?.textCode)
        assertEquals(customMessage, error?.ev?.message)
        assertEquals("config.hold_matrix_test.holdable", error?.ev?.failPart)
        assertFalse(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_placeHold_failWithHoldExists() {
        val json = """
            {"payload":[{"target":210,"result":[{"ilsevent":"1707","desc":"User already has an open hold on the selected item","pid":9379,"textcode":"HOLD_EXISTS"}]}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("User already has an open hold on the selected item", result.errorMessage)
    }

    /// This hold response has a payload with a result that is an event (not a list of events)
    @Test
    fun test_placeHold_failWithDatabaseError() {
        val json = """
            {"payload":[{"result":{"payload":{"__c":"au","__p":["f","luser",69]},"textcode":"DATABASE_UPDATE_FAILED","desc":"The attempt to write to the DB failed","ilsevent":"2001"},"target":4438693}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("The attempt to write to the DB failed", result.errorMessage)
    }

    // This hold response has a result containing an auto-generated last_event, with an empty "ilsevent" and "desc".
    // The OPAC gets the error message from a cascading series of checks that ends up with "Problem: STAFF_CHR".
    @Test
    fun test_placeHold_failWithAlertBlock() {
        val json = """
            {"payload":[{"result":{"place_unfillable":0,"last_event":{"servertime":"Fri Mar 15 14:40:20 2024","payload":{"fail_part":"STAFF_CHR"},"pid":1337988,"ilsevent":"","stacktrace":"Holds.pm:3370","textcode":"STAFF_CHR","desc":""},"success":0,"age_protected_copy":0},"target":6390231}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("STAFF_CHR", result.errorMessage)
    }

    @Test
    fun test_titleHoldIsPossible_fail() {
        val json = """
            {"payload":[{"last_event":{"ilsevent":"1714","textcode":"HIGH_LEVEL_HOLD_HAS_NO_COPIES","desc":"A hold request at a higher level than copy has been attempted, but there are no copies that belonging to the higher-level unit.","payload":{"fail_part":"no_ultimate_items"}},"place_unfillable":1,"age_protected_copy":null,"success":0}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("The system could not find any items to match this hold request", result.errorMessage)
    }

    // Test to handle https://bugs.launchpad.net/opensrf/+bug/1883169
    @Test
    fun test_crashResponseNotJSON() {
        val jsonIsh = """
            {"payload":[],"debug": "...NULL,NULL F,"status":500}
            """
        val result = GatewayResult.create(jsonIsh)
        assertTrue(result.failed)
        assertEquals("Internal Server Error: response is not JSON", result.errorMessage)
    }

    @Test
    fun test_shouldCache() {
        val tests = listOf<Pair<String, Boolean>>(
                Pair("""
                     {"payload":[{"result":6309896,"target":21296176}],"status":200}
                     """.trimIndent(), true),
                Pair("""
                     {"payload":[[]],"status":200}
                     """.trimIndent(), true),
                Pair("""
                     {"payload":[],"status":200}
                     """.trimIndent(), false),
                Pair("""
                     {"payload":[],"status":400}
                     """.trimIndent(), false),
        )
        for (test in tests) {
            val result = GatewayResult.create(test.first)
            assertEquals(test.second, result.shouldCache)
        }
    }
}
