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

import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.data.jsonMapOf
import org.evergreen_ils.system.EvergreenServer
import org.evergreen_ils.system.Organization
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class OrganizationTest {

    val eg = EvergreenServer.getInstance()

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    fun setUpOrgTypes() {
        val orgTypeConsortium = OSRFObject(jsonMapOf(
                "id" to 1,
                "name" to "Consortium",
                "opac_label" to "All Libraries in Our Network",
                "can_have_users" to "f",
                "can_have_vols" to "f"
        ))
        val orgTypeLibrary = OSRFObject(jsonMapOf(
                "id" to 3,
                "name" to "Library",
                "opac_label" to "This Library",
                "can_have_users" to "t",
                "can_have_vols" to "t"
        ))
        val orgTypeSystem = OSRFObject(jsonMapOf(
                "id" to 2,
                "name" to "System",
                "opac_label" to "All Branches of This Library",
                "can_have_users" to "f",
                "can_have_vols" to "f"
        ))
        val orgTypes = arrayListOf(orgTypeConsortium, orgTypeLibrary, orgTypeSystem)
        eg.loadOrgTypes(orgTypes)
    }

    fun setUpOrgs() {
        val branchObj = OSRFObject(jsonMapOf(
                "id" to 29,
                "ou_type" to 3,
                "shortname" to "BETHEL",
                "name" to "Bethel Public Library",
                "opac_visible" to "t",
                "parent_ou" to 28,
                "children" to null
        ))
        val systemObj = OSRFObject(jsonMapOf(
                "id" to 28,
                "ou_type" to 2,
                "shortname" to "BETSYS",
                "name" to "Bethel",
                "opac_visible" to "f",
                "parent_ou" to 1,
                "children" to arrayListOf(branchObj)
        ))
        val consortiumObj = OSRFObject(jsonMapOf(
                "id" to 1,
                "ou_type" to 1,
                "shortname" to "CONS",
                "name" to "Bibliomation",
                "opac_visible" to "t",
                "parent_ou" to null,
                "children" to arrayListOf(systemObj)
        ))
        eg.loadOrganizations(consortiumObj, true)
    }

    fun setUp() {
        setUpOrgTypes()
        setUpOrgs()
    }

    fun getSpinnerLabels(): ArrayList<String> {
        var list = java.util.ArrayList<String>()
        for (org in eg.organizations) {
            list.add(org.treeDisplayName)
        }
        return list
    }

    @Test
    fun test_loadOrgTypes() {
        // no orgs yet
        assertEquals(0, eg.orgTypes.size)
        assertNull(eg.findOrgType(1))

        setUpOrgTypes()
        assertEquals(3, eg.orgTypes.size)

        val topOrgType = eg.findOrgType(1)
        assertEquals(topOrgType?.name, "Consortium")
    }

    @Test
    fun test_loadOrganizations() {
        setUp()

        assert(eg.organizations.isNotEmpty())
    }

    @Test
    fun test_findOrg() {
        setUp()

        val lib = eg.getOrganization(29)
        assertEquals("BETHEL", lib?.shortname)

        assertNull(eg.getOrganization(999))
    }

    @Test
    fun test_findOrgByShortName() {
        setUp()

        val lib = eg.getOrganizationByShortName("BETHEL")
        assertEquals(29, lib?.id)
    }

    @Test
    fun test_spinnerLabels() {
        setUp()

        val lib = eg.getOrganizationByShortName("BETHEL")
        assertEquals("   ", lib?.indentedDisplayPrefix)

        val labels = getSpinnerLabels()
        assertEquals(arrayListOf(
                "Bibliomation",
                "   Bethel Public Library"
        ), labels)
    }



    @Test
    fun test_invisibleOrgsAreLoaded() {
        setUp()

        assertEquals(3, eg.organizations.size)

        val lib = eg.getOrganization(29)
        assertEquals(true, lib?.opac_visible)
        assertEquals("BETHEL", lib?.shortname)
        assert(lib!!.orgType.can_have_users)
        assert(lib!!.orgType.can_have_vols)

        val system = eg.getOrganization(28)
        assertEquals(false, lib?.opac_visible)
        assertEquals("BETSYS", lib?.shortname)
        assertFalse(system!!.orgType.can_have_users)
        assertFalse(system!!.orgType.can_have_vols)

        val cons = eg.getOrganization(1)
        assertEquals(true, lib?.opac_visible)
        assertEquals("CONS", lib?.shortname)
        assertFalse(cons!!.orgType.can_have_users)
        assertFalse(cons!!.orgType.can_have_vols)
    }

    @Test
    fun test_orgAncestry() {
        setUp()

        val libAncestry = eg.getOrganizationAncestry("BETHEL")
        assertEquals(arrayListOf("BETHEL", "BETSYS", "CONS"), libAncestry)
    }
}
