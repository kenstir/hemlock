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

import org.evergreen_ils.system.EgCopyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.GatewayResult
import org.opensrf.util.OSRFRegistry

class CopyStatusTest {
    val ccsListJson = """
{"payload":[[
{"__c":"ccs","__p":["f",4,"Missing","f","f","f","f"]},
{"__c":"ccs","__p":["t",5,"In process","t","f","f","f"]},
{"__c":"ccs","__p":["t",1,"Checked out","t","t","t","f"]},
{"__c":"ccs","__p":["t",8,"On holds shelf","t","t","t","f"]},
{"__c":"ccs","__p":["t",0,"Available","t","t","f","t"]}
]],"status":200}
"""

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val ccsFields = arrayOf("holdable","id","name","opac_visible","copy_active","restrict_copy_delete","is_available")
            OSRFRegistry.registerObject("ccs", OSRFRegistry.WireProtocol.ARRAY, ccsFields)
        }
    }

    @Test
    fun test_load() {
        val result = GatewayResult.create(ccsListJson)
        val ccsList = result.payloadFirstAsObjectList()
        EgCopyStatus.loadCopyStatuses(ccsList)

        assertEquals(5, ccsList.size)
        assertEquals(4, EgCopyStatus.copyStatusList.size)

        val ccs = EgCopyStatus.find(0)
        assertEquals("Available", ccs?.name)

        assertNull(EgCopyStatus.find(4))
    }
}
