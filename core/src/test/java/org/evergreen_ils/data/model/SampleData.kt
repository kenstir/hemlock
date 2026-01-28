/*
 * Copyright (c) 2026 Kenneth H. Cox
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

import net.kenstir.data.jsonMapOf
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.system.EgOrg

object SampleData {
    fun loadOrgTypes() {
        val cons = OSRFObject(jsonMapOf(
            "id" to 1,
            "name" to "Consortium",
            "opac_label" to "Everywhere",
            "depth" to 0,
            "parent" to null,
            "can_have_vols" to "f",
            "can_have_users" to "f",
        ))
        val system = OSRFObject(jsonMapOf(
            "id" to 2,
            "name" to "System",
            "opac_label" to "Local Library System",
            "depth" to 1,
            "parent" to 1,
            "can_have_vols" to "f",
            "can_have_users" to "f",
        ))
        val branch = OSRFObject(jsonMapOf(
            "id" to 3,
            "name" to "Branch",
            "opac_label" to "This Branch",
            "depth" to 2,
            "parent" to 2,
            "can_have_vols" to "t",
            "can_have_users" to "t",
        ))
        val subLibrary = OSRFObject(jsonMapOf(
            "id" to 4,
            "name" to "Sub-library",
            "opac_label" to "This Specialized Library",
            "depth" to 3,
            "parent" to 3,
            "can_have_vols" to "t",
            "can_have_users" to "t",
        ))
        val bookmobile = OSRFObject(jsonMapOf(
            "id" to 5,
            "name" to "Bookmobile",
            "opac_label" to "Your Bookmobile",
            "depth" to 3,
            "parent" to 3,
            "can_have_vols" to "t",
            "can_have_users" to "t",
        ))
        EgOrg.loadOrgTypes(listOf(cons, system, branch, subLibrary, bookmobile))
    }

    fun loadOrgs() {
        val br1 = OSRFObject(jsonMapOf(
            "id" to 4,
            "ou_type" to 3,
            "shortname" to "BR1",
            "name" to "Example Branch 1",
            "opac_visible" to "t",
            "parent_ou" to 2,
            "children" to null))
        val br2 = OSRFObject(jsonMapOf(
            "id" to 5,
            "ou_type" to 3,
            "shortname" to "BR2",
            "name" to "Example Branch 2",
            "opac_visible" to "t",
            "parent_ou" to 2,
            "children" to null))
        val sys1 = OSRFObject(jsonMapOf(
            "id" to 2,
            "ou_type" to 2,
            "shortname" to "SYS1",
            "name" to "Example System 1",
            "opac_visible" to "t",
            "parent_ou" to 1,
            "children" to listOf(br1, br2)))
        val br3 = OSRFObject(jsonMapOf(
            "id" to 6,
            "ou_type" to 3,
            "shortname" to "BR3",
            "name" to "Example Branch 3",
            "opac_visible" to "t",
            "parent_ou" to 3,
            "children" to null))
        val br4 = OSRFObject(jsonMapOf(
            "id" to 7,
            "ou_type" to 3,
            "shortname" to "BR4",
            "name" to "Example Branch 4",
            "opac_visible" to "f",
            "parent_ou" to 3,
            "children" to null))
        val sys2 = OSRFObject(jsonMapOf(
            "id" to 3,
            "ou_type" to 2,
            "shortname" to "SYS2",
            "name" to "Example System 2",
            "opac_visible" to "t",
            "parent_ou" to 1,
            "children" to listOf(br3, br4)))
        val cons = OSRFObject(jsonMapOf(
            "id" to 1,
            "ou_type" to 1,
            "shortname" to "CONS",
            "name" to "Example Consortium",
            "opac_visible" to "t",
            "parent_ou" to null,
            "children" to listOf(sys1, sys2)))
        EgOrg.loadOrgs(cons, true)
    }

    fun make_ccs(id: Int, name: String, opac_visible: String): OSRFObject {
        return OSRFObject(jsonMapOf("id" to id, "name" to name, "opac_visible" to opac_visible))
    }

    fun loadCopyStatuses() {
        val ccsList = listOf(
            make_ccs(0, "Available", "t"),
            make_ccs(1, "Checked out", "t"),
            make_ccs(7, "Reshelving", "t"),
        )
        EgCopyStatus.loadCopyStatuses(ccsList)
    }
}
