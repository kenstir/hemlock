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

import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class SerializationPerformanceTest {
    companion object {
        const val iterations = 100
        lateinit var largeOrgTreeJson: String
    }

    @Before
    fun setup() {
        largeOrgTreeJson = javaClass
            .classLoader
            ?.getResource("largeOrgTree.json")!!
            .readText(StandardCharsets.UTF_8)

        OSRFCoder.clearRegistry()
        OSRFCoder.registerClass("aou", listOf("children","billing_address","holds_address","id","ill_address","mailing_address","name","ou_type","parent_ou","shortname","email","phone","opac_visible","fiscal_calendar","users","closed_dates","circulations","settings","addresses","checkins","workstations","fund_alloc_pcts","copy_location_orders","atc_prev_dests","resv_requests","resv_pickups","rsrc_types","resources","rsrc_attrs","attr_vals","hours_of_operation","fscskey","fscs_seq","libpas","library_logo","oclc_symbol"))
    }

    @Test
    fun test_perf_kotlinreader() {
        val json = largeOrgTreeJson
        for (i in 0 until iterations) {
            val resp = Json.decodeFromString<XGatewayResponseContent>(json)
            val list = OSRFCoder.decodePayload(resp.payload)
            assertNotNull(list)
        }
    }
}
