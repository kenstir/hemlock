/*
 * Copyright (C) 2017 Kenneth H. Cox
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
package net.kenstir.apps.core

import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class OrganizationTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    fun setUpOrgTypes() {
        val orgType = OSRFObject()
        orgType["name"] = "Consortium"
        orgType["id"] = 1
        orgType["opac_label"] = "All PINES Libraries"
        orgType["can_have_users"] = "f"
        orgType["can_have_vols"] = "f"
        val orgTypes = listOf<OSRFObject>(orgType)
        EgOrg.loadOrgTypes(orgTypes)
    }

    @Test
    fun test_loadOrgTypes() {
        // no orgs yet
        assertEquals(0, EgOrg.orgTypes.size)
        assertNull(EgOrg.findOrgType(1))

        setUpOrgTypes()
        val topOrgType = EgOrg.findOrgType(1)
        assertEquals(topOrgType?.name, "Consortium")

        /*
        Assert.assertNull(EvergreenService.getOrganization(null))
        Assert.assertNull(eg!!.getOrganization(1))
        // add org
        val o = OSRFObject()
        o["name"] = String("Example Consortium")
        o["ou_type"] = 1
        o["opac_visible"] = String("t")
        o["parent_ou"] = null
        o["id"] = 1
        o["shortname"] = String("CONS")
        eg!!.loadOrganizations(o, true)
        // now we can find it
        val org = eg!!.getOrganization(1)
        org.junit.Assert.assertEquals(1, org.id)
        org.junit.Assert.assertEquals("CONS", org.shortname)
        // misc
        org.junit.Assert.assertEquals("Example Consortium", eg!!.getOrganizationName(1))
        org.junit.Assert.assertEquals("", eg!!.getOrganizationName(2))
        org.junit.Assert.assertNull(eg!!.getOrganization(2))
        */
    }
}
