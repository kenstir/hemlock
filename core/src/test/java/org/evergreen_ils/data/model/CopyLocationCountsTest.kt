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

package org.evergreen_ils.data.model

import net.kenstir.util.visibleCopyLocationCounts
import org.evergreen_ils.data.service.EvergreenConsortiumService
import org.evergreen_ils.gateway.GatewayResult
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class CopyLocationCountsTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            SampleData.loadOrgTypes()
            SampleData.loadOrgs()
            SampleData.loadCopyStatuses()
        }
    }

    @Test
    fun test_oneCheckedOut() {
        val cscJson = """
            {"payload":[[["7","","DVD HARRY","","AV",{"1":1}]]],"status":200}
            """
        val payloadList = GatewayResult.create(cscJson).payloadFirstAsList()
        assertEquals(1, payloadList.size)
        val clcList = EvergreenCopyLocationCounts.makeArray(payloadList)
        val clc = clcList.first() as? EvergreenCopyLocationCounts
            ?: throw AssertionError("Expected EvergreenCopyLocationCounts instance")
        assertEquals(7, clc.orgId)
        assertEquals("", clc.callNumberPrefix)
        assertEquals("DVD HARRY", clc.callNumberLabel)
        assertEquals("", clc.callNumberSuffix)
        assertEquals("AV", clc.copyLocation)
        assertEquals(1, clc.countsByStatus.size)
        assertEquals("1 Checked out", clc.countsByStatusLabel)
        assertEquals("DVD HARRY", clc.callNumber)
    }

    @Test
    fun test_someAvailable() {
        val cscJson = """
            {"payload":[[["7","","J ROWLING","","JUV",{"1":2,"7":1}],["7","","YA ROWLING","","YA",{"1":1,"0":3}]]],"status":200}
            """
        val payloadList = GatewayResult.create(cscJson).payloadFirstAsList()
        val clcList = EvergreenCopyLocationCounts.makeArray(payloadList)
        assertEquals(2, clcList.size)
        val clc1 = clcList[0] as EvergreenCopyLocationCounts
        val clc2 = clcList[1] as EvergreenCopyLocationCounts
        assertEquals(listOf(Pair(1, 2), Pair(7,1)), clc1.countsByStatus)
        assertEquals("2 Checked out\n1 Reshelving", clc1.countsByStatusLabel)
        assertEquals("3 Available\n1 Checked out", clc2.countsByStatusLabel)
    }

    @Test
    fun test_ignoreCopyFromNonVisibleOrg() {
        // orgID 7 is not visible
        val jsonPayload = """
            [[["4","","YA B JOHNSON","","NONFIC",{"0":1}],
              ["5","","YA B JOHNSON","","NONFIC",{"1":1}],
              ["7","","YA B JOHNSON","","NONFIC",{"0":1}]]]
            """
        val cscJson = """
            {"payload":$jsonPayload,"status":200}
            """.trimIndent()
        val payloadList = GatewayResult.create(cscJson).payloadFirstAsList()
        val clcList = EvergreenCopyLocationCounts.makeArray(payloadList)
        assertEquals(3, clcList.size)

        val consortiumService = EvergreenConsortiumService
        val visible = visibleCopyLocationCounts(clcList, consortiumService)
        assertEquals(2, visible.size)
        assertEquals(null, visible.find { it.orgId == 7 } )
    }
}
