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
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.android.StdoutLogProvider
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.datax.XOSRFObject
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class OrganizationTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    fun setUpOrgTypes() {
        val orgTypeConsortium = XOSRFObject(
            jsonMapOf(
                "id" to 1,
                "name" to "Consortium",
                "opac_label" to "All Libraries in Our Network",
                "can_have_users" to "f",
                "can_have_vols" to "f"
        )
        )
        val orgTypeLibrary = XOSRFObject(
            jsonMapOf(
                "id" to 3,
                "name" to "Library",
                "opac_label" to "This Library",
                "can_have_users" to "t",
                "can_have_vols" to "t"
        )
        )
        val orgTypeSystem = XOSRFObject(
            jsonMapOf(
                "id" to 2,
                "name" to "System",
                "opac_label" to "All Branches of This Library",
                "can_have_users" to "f",
                "can_have_vols" to "f"
        )
        )
        val orgTypes = listOf(orgTypeConsortium, orgTypeLibrary, orgTypeSystem)
        EgOrg.loadOrgTypes(orgTypes)
    }

    fun setUpOrgs() {
        val branchObj = XOSRFObject(
            jsonMapOf(
                "id" to 29,
                "ou_type" to 3,
                "shortname" to "BETHEL",
                "name" to "Bethel Public Library",
                "opac_visible" to "t",
                "parent_ou" to 28,
                "children" to null
        )
        )
        val systemObj = XOSRFObject(
            jsonMapOf(
                "id" to 28,
                "ou_type" to 2,
                "shortname" to "BETSYS",
                "name" to "Bethel",
                "opac_visible" to "f",
                "parent_ou" to 1,
                "children" to arrayListOf(branchObj)
        )
        )
        val consortiumObj = XOSRFObject(
            jsonMapOf(
                "id" to 1,
                "ou_type" to 1,
                "shortname" to "CONS",
                "name" to "Bibliomation",
                "opac_visible" to "t",
                "parent_ou" to null,
                "children" to arrayListOf(systemObj)
        )
        )
        EgOrg.loadOrgs(consortiumObj, true)
    }

    fun setUp() {
        setUpOrgTypes()
        setUpOrgs()
    }

    @Test
    fun test_loadOrgTypes() {
        setUpOrgTypes()
        assertEquals(3, EgOrg.orgTypes.size)

        val topOrgType = EgOrg.findOrgType(1)
        assertEquals(topOrgType?.name, "Consortium")

        assertNull(EgOrg.findOrgType(999))
    }

    @Test
    fun test_loadOrganizations() {
        setUp()

        assertTrue(EgOrg.allOrgs.isNotEmpty())
    }

    @Test
    fun test_findOrg() {
        setUp()

        val lib = EgOrg.findOrg(29)
        assertEquals("BETHEL", lib?.shortname)

        assertNull(EgOrg.findOrg(999))
    }

    @Test
    fun test_findOrgByShortName() {
        setUp()

        val lib = EgOrg.findOrgByShortName("BETHEL")
        assertEquals(29, lib?.id)
    }

    @Test
    fun test_spinnerLabels() {
        setUp()

        val lib = EgOrg.findOrgByShortName("BETHEL")
        assertEquals("   ", lib?.indentedDisplayPrefix)

        val labels = EgOrg.orgSpinnerLabels()
        assertEquals(arrayListOf("Bibliomation", "   Bethel Public Library"), labels)
    }

    /*
    @Test
    fun test_getOrganizationSpinnerLabelsAndSelectedIndex() {
        setUp()

        var pair = eg.getOrganizationSpinnerLabelsAndSelectedIndex(null)
        assertEquals(arrayListOf("Bibliomation", "   Bethel Public Library"), pair.first)
        assertEquals(0, pair.second) // 0 here means not found

        pair = eg.getOrganizationSpinnerLabelsAndSelectedIndex(1)
        assertEquals(0, pair.second) // 0 here is found
        assertEquals(1, eg.visibleOrganizations.get(pair.second!!).id)

        pair = eg.getOrganizationSpinnerLabelsAndSelectedIndex(28)
        assertEquals(0, pair.second) // 0 here means not found

        pair = eg.getOrganizationSpinnerLabelsAndSelectedIndex(29)
        assertEquals(1, pair.second)
        assertEquals(29, eg.visibleOrganizations.get(pair.second!!).id)
    }

     */

    @Test
    fun test_invisibleOrgsAreLoaded() {
        setUp()

        assertEquals(3, EgOrg.allOrgs.size)
        assertEquals(2, EgOrg.visibleOrgs.size)

        val lib = EgOrg.findOrg(29)
        assertEquals(true, lib?.opacVisible)
        assertEquals("BETHEL", lib?.shortname)
        assertTrue(lib!!.orgType!!.canHaveUsers)
        assertTrue(lib.orgType!!.canHaveVols)

        val system = EgOrg.findOrg(28)
        assertEquals(false, system?.opacVisible)
        assertEquals("BETSYS", system?.shortname)
        assertFalse(system!!.orgType!!.canHaveUsers)
        assertFalse(system.orgType!!.canHaveVols)

        val cons = EgOrg.findOrg(1)
        assertEquals(true, cons?.opacVisible)
        assertEquals("CONS", cons?.shortname)
        assertFalse(system.orgType!!.canHaveUsers)
        assertFalse(system.orgType!!.canHaveVols)
    }

    @Test
    fun test_orgAncestry() {
        setUp()

        val libAncestry = EgOrg.getOrgAncestry("BETHEL")
        assertEquals(arrayListOf("BETHEL", "BETSYS", "CONS"), libAncestry)
    }
}
