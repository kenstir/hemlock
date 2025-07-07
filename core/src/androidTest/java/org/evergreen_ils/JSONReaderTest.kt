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

package org.evergreen_ils

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import org.evergreen_ils.xdata.XGatewayResponseContent
import org.evergreen_ils.xdata.XOSRFCoder
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.opensrf.util.JSONReader
import org.opensrf.util.OSRFRegistry
import java.nio.charset.StandardCharsets

class JSONReaderTest {
    companion object {
        const val iterations = 100
        lateinit var largeOrgTreeJson: String
    }

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val inStream = ctx.resources.assets.open("largeOrgTree.json")
        largeOrgTreeJson = inStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        OSRFRegistry.registerObject("aou", OSRFRegistry.WireProtocol.ARRAY, arrayOf("children","billing_address","holds_address","id","ill_address","mailing_address","name","ou_type","parent_ou","shortname","email","phone","opac_visible","fiscal_calendar","users","closed_dates","circulations","settings","addresses","checkins","workstations","fund_alloc_pcts","copy_location_orders","atc_prev_dests","resv_requests","resv_pickups","rsrc_types","resources","rsrc_attrs","attr_vals","hours_of_operation","fscskey","fscs_seq","libpas","library_logo","oclc_symbol"))

        XOSRFCoder.clearRegistry()
        XOSRFCoder.registerClass("aou", listOf("children","billing_address","holds_address","id","ill_address","mailing_address","name","ou_type","parent_ou","shortname","email","phone","opac_visible","fiscal_calendar","users","closed_dates","circulations","settings","addresses","checkins","workstations","fund_alloc_pcts","copy_location_orders","atc_prev_dests","resv_requests","resv_pickups","rsrc_types","resources","rsrc_attrs","attr_vals","hours_of_operation","fscskey","fscs_seq","libpas","library_logo","oclc_symbol"))
    }

    @Test
    fun test_perf_jsonreader() {
        val json = largeOrgTreeJson
        for (i in 0 until iterations) {
            val response = JSONReader(json).readObject()
            val obj = response["payload"]
            assertNotNull(obj)
        }
    }

    @Test
    fun test_perf_kotlinreader() {
        val json = largeOrgTreeJson
        for (i in 0 until iterations) {
            val resp = Json.decodeFromString<XGatewayResponseContent>(json)
            val list = XOSRFCoder.decodePayload(resp.payload)
            assertNotNull(list)
        }
    }

}
