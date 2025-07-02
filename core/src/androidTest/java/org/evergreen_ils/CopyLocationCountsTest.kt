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

package org.evergreen_ils

import org.evergreen_ils.xdata.XOSRFObject
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.data.EvergreenCopyLocationCounts
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.xdata.XGatewayResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.opensrf.util.GatewayResult

class CopyLocationCountsTest {

    fun make_ccs(id: Int, name: String, opac_visible: String): XOSRFObject {
        return XOSRFObject(jsonMapOf("id" to id, "name" to name, "opac_visible" to opac_visible))
    }

    @Before
    fun setUp() {
        val ccsList = listOf(
            make_ccs(1, "Checked out", "t"),
            make_ccs(0, "Available", "t"),
            make_ccs(7, "Reshelving", "t")
        )
        EgCopyStatus.loadCopyStatuses(ccsList)
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
        val payloadList = XGatewayResult.create(cscJson).payloadFirstAsList()
        val clcList = EvergreenCopyLocationCounts.makeArray(payloadList)
        assertEquals(2, clcList.size)
        val clc1 = clcList[0] as EvergreenCopyLocationCounts
        val clc2 = clcList[1] as EvergreenCopyLocationCounts
        assertEquals(listOf(Pair(1, 2), Pair(7,1)), clc1.countsByStatus)
        assertEquals("2 Checked out\n1 Reshelving", clc1.countsByStatusLabel)
        assertEquals("3 Available\n1 Checked out", clc2.countsByStatusLabel)
    }
}
