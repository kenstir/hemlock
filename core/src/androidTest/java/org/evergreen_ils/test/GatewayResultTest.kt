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
import org.evergreen_ils.Api
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
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

            val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
            EgMessageMap.init(resources)
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
    fun test_shortObject() {
        // decoding an object where the wire protocol has fewer elements than the class has fields
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser"]}]}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asObject()
        assertNotNull(obj)
        assertEquals(false, Api.parseBoolean(obj.get("juvenile")))
        assertEquals("luser", obj.getString("usrname"))
        assertEquals(null, obj.getInt("home_ou"))
    }

    @Test
    fun test_tooManyFields() {
        // decoding an object where the wire protocol has more elements than the class has fields
        val json = """
            {"status":200,"payload":[{"__c":"au","__p":["f","luser", 69, "Unexpected"]}]}
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
        assertEquals(3, obj.getInt("total_holds"))
        assertEquals(2, obj.getInt("queue_position"))
    }

    @Test
    fun test_classlessArray() {
        val json = """
             {"payload":[[{"label":"JAN 2005","record":2224604}]],"status":200}
             """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asArray()
        assertEquals(1, obj.size)

        val objArray = result.asObjectArray()
        assertEquals(1, objArray.size)
        val first = objArray.firstOrNull()
        assertNotNull(first)
        if (first == null) return
        assertEquals("JAN 2005", first.getString("label"))
        assertEquals(2224604, first.getInt("record"))
    }

    @Test
    fun test_failureOnBadStatus() {
        val json = """
            {"payload":[],"status":400}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)

        val errorMessage = "Network request failed (status:400)"
        val objResult = kotlin.runCatching { result.asObject() }
        assertTrue(objResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
        val strResult = kotlin.runCatching { result.asString() }
        assertTrue(strResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
        val arrayResult = kotlin.runCatching { result.asObjectArray() }
        assertTrue(arrayResult.isFailure)
        assertEquals(errorMessage, objResult.exceptionOrNull()?.message)
    }

    // This does happens IRL but it's a server error
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
        assertEquals("Server error: expected object, got EMPTY", objResult.exceptionOrNull()?.message)
        val strResult = kotlin.runCatching { result.asString() }
        assertTrue(strResult.isFailure)
        assertEquals("Server error: expected string, got EMPTY", strResult.exceptionOrNull()?.message)
        val arrayResult = kotlin.runCatching { result.asObjectArray() }
        assertTrue(arrayResult.isFailure)
        assertEquals("Server error: expected array, got EMPTY", arrayResult.exceptionOrNull()?.message)
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
        assertEquals(error?.ev?.failPart, null)
        assertTrue(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_failureWithEventList() {
        // This payload is an array of events
        val json = """
            {"payload":[[{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_FINES"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"7013","pid":23476,"textcode":"PATRON_EXCEEDS_FINES","desc":"The patron in question has reached the maximum fine amount"},{"stacktrace":"...","payload":{"fail_part":"PATRON_EXCEEDS_LOST_COUNT"},"servertime":"Mon Nov 25 20:57:11 2019","ilsevent":"1236","pid":23476,"textcode":"PATRON_EXCEEDS_LOST_COUNT","desc":"The patron has too many lost items."}]],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        val customMessage = "Patron has reached the maximum fine amount" // event_msg_map.json
        assertEquals(customMessage, result.errorMessage)

        val res = kotlin.runCatching { result.asObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals(7013, error?.ev?.code)
        assertEquals("PATRON_EXCEEDS_FINES", error?.ev?.textCode)
        assertEquals(customMessage, error?.ev?.message)
        assertFalse(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_placeHold_failWithFailPart() {
        // This payload is an object, containing a result object, containing a last_event event
        // I have only seen such a response from open-ils.circ.holds.test_and_create.batch
        val json = """
            {"payload":[{"result":{"success":0,"last_event":{"servertime":"Mon May  4 20:26:18 2020","payload":{"fail_part":"config.hold_matrix_test.holdable"},"stacktrace":"Holds.pm:3028","textcode":"ITEM_NOT_HOLDABLE","pid":2131,"ilsevent":"1220","desc":"A copy with a remote circulating library (circ_lib) was encountered"},"age_protected_copy":0,"place_unfillable":0},"target":6249829}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        val customMessage = "Hold rules reject this item as unholdable" // fail_part_msg_map.json
        assertEquals(customMessage, result.errorMessage)

        val res = kotlin.runCatching { result.asObject() }
        assertTrue(res.isFailure)
        val error = res.exceptionOrNull() as? GatewayEventError
        assertEquals(1220, error?.ev?.code)
        assertEquals("ITEM_NOT_HOLDABLE", error?.ev?.textCode)
        assertEquals(customMessage, error?.ev?.message)
        assertEquals("config.hold_matrix_test.holdable", error?.ev?.failPart)
        assertFalse(error?.isSessionExpired() ?: false)
    }

    @Test
    fun test_placeHold_failWithHoldExists() {
        val json = """
            {"payload":[{"target":210,"result":[{"ilsevent":"1707","servertime":"Sun Aug  2 18:19:53 2020","stacktrace":"Holds.pm:302","desc":"User already has an open hold on the selected item","pid":9379,"textcode":"HOLD_EXISTS"}]}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("User already has an open hold on the selected item", result.errorMessage)
    }

    @Test
    fun test_titleHoldIsPossible_fail() {
        val json = """
            {"payload":[{"last_event":{"ilsevent":"1714","servertime":"Sat Aug 22 09:45:28 2020","pid":6842,"textcode":"HIGH_LEVEL_HOLD_HAS_NO_COPIES","stacktrace":"Holds.pm:2617","desc":"A hold request at a higher level than copy has been attempted, but there are no copies that belonging to the higher-level unit.","payload":{"fail_part":"no_ultimate_items"}},"place_unfillable":1,"age_protected_copy":null,"success":0}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertTrue(result.failed)
        assertEquals("The system could not find any items to match this hold request", result.errorMessage)
    }

    @Test
    fun test_placeHold_success() {
        val json = """
            {"payload":[{"result":6309896,"target":21296176}],"status":200}
            """
        val result = GatewayResult.create(json)
        assertFalse(result.failed)

        val obj = result.asObject()
        assertEquals(obj.getInt("result"), 6309896)
    }

    // Test to handle https://bugs.launchpad.net/opensrf/+bug/1883169
    @Test
    fun test_errorResponseNotJSON_postgresQueryKilled() {
        val jsonIsh = """
            {"payload":[],"debug": "osrfMethodException :  *** Call to [open-ils.search.biblio.multiclass.query] failed for session [1590536419.970333.159053641999519], thread trace [1]:\nException: OpenSRF::EX::ERROR 2020-05-26T19:41:01 OpenSRF::Application /usr/local/share/perl/5.22.1/OpenSRF/Application.pm:243 System ERROR: Call to open-ils.storage for method open-ils.storage.biblio.multiclass.staged.search_fts.atomic \n failed with exception: Exception: OpenSRF::EX::ERROR 2020-05-26T19:41:01 OpenILS::Application::AppUtils /usr/local/share/perl/5.22.1/OpenILS/Application/AppUtils.pm:201 System ERROR: Exception: OpenSRF::DomainObject::oilsMethodException 2020-05-26T19:41:01 OpenSRF::AppRequest /usr/local/share/perl/5.22.1/OpenSRF/AppSession.pm:1159 <500>   *** Call to [open-ils.storage.biblio.multiclass.staged.search_fts.atomic] failed for session [1590536419.97576725.9316069925], thread trace [1]:\nDBD::Pg::st execute failed: ERROR:  canceling statement due to user request [for Statement ... COUNT(*),COUNT(*),COUNT(*),0,0,NULL,NULL F,"status":500}
            """
        val result = GatewayResult.create(jsonIsh)
        assertTrue(result.failed)
        assertEquals("Timeout; the request took too long to complete and the server killed it", result.errorMessage)
    }

    @Test
    fun test_errorResponseNotJSON_other() {
        val jsonIsh = """
            {"payload":[],"debug": "...NULL,NULL F,"status":500}
            """
        val result = GatewayResult.create(jsonIsh)
        assertTrue(result.failed)
        assertEquals("Server error: response is not JSON", result.errorMessage)
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
