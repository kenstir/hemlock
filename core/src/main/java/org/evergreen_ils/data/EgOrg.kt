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

package org.evergreen_ils.data

import org.evergreen_ils.Api
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.Organization
import org.opensrf.util.OSRFObject
import java.util.*
import kotlin.Comparator

object EgOrg {
    @JvmStatic
    var orgTypes = mutableListOf<OrgType>()
    @JvmStatic
    var orgs = mutableListOf<Organization>()
    @JvmStatic
    var smsEnabled = false
    @JvmStatic
    val consortiumID = 1

    fun loadOrgTypes(objArray: List<OSRFObject>) {
        synchronized(this) {
            orgTypes.clear()
            objArray.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    val orgType = OrgType(id,
                            obj.getString("name"),
                            obj.getString("opac_label"),
                            obj.getBoolean("can_have_users"),
                            obj.getBoolean("can_have_vols"))
                    orgTypes.add(orgType)
                }
            }
        }
        Log.d(org.evergreen_ils.api.TAG, "loadOrgTypes: ${objArray.size} org types")
    }

    @JvmStatic
    fun findOrgType(id: Int): OrgType? {
        return orgTypes.firstOrNull { it.id == id }
    }

    private fun addOrganization(obj: OSRFObject, level: Int) {
        val id = obj.getInt("id")
        val orgType = obj.getInt("ou_type")
        if (id == null) return
        if (orgType == null) return
        val org = Organization()
        org.level = level
        org.id = id
        org.parent_ou = obj.getInt("parent_ou")
        org.name = obj.getString("name")
        org.shortname = obj.getString("shortname")
        org.orgType = orgType
        org.opac_visible = Api.parseBoolean(obj.getString("opac_visible"))
        org.indentedDisplayPrefix = String(CharArray(level)).replace("\u0000", "   ")
        //Log.d(TAG, "id:$id level:${org.level} vis:${org.opac_visible} shortname:${org.shortname} name:${org.name}")
        if (org.opac_visible)
            orgs.add(org)
        val children = obj.get("children") as? List<OSRFObject>
        children?.forEach { child ->
            val child_level = if (org.opac_visible) level + 1 else level
            addOrganization(child, child_level)
        }
    }

    fun loadOrgs(orgTree: OSRFObject, hierarchical_org_tree: Boolean) {
        synchronized(this) {
            orgs.clear()
            addOrganization(orgTree, 0)
            // If the org tree is too big, then an indented list is unwieldy.
            // Convert it into a flat list sorted by org.name.
            if (!hierarchical_org_tree && orgs.size > 25) {
                Collections.sort(orgs, Comparator<Organization> { a, b ->
                    // top-level OU appears first
                    if (a.level == 0) return@Comparator -1
                    if (b.level == 0) 1 else a.name.compareTo(b.name)
                })
                for (o in orgs) {
                    o.indentedDisplayPrefix = ""
                }
            }
        }
        Log.d(org.evergreen_ils.api.TAG, "loadOrgs: ${orgs.size} orgs")
    }

    @JvmStatic
    fun findOrg(id: Int): Organization? = orgs.firstOrNull { it.id == id }

    @JvmStatic
    fun getOrgShortNameSafe(id: Int): String = findOrg(id)?.shortname ?: "?"

    @JvmStatic
    fun getOrgNameSafe(id: Int): String = findOrg(id)?.name ?: "?"

    @JvmStatic
    fun findOrgByShortName(shortName: String): Organization? = orgs.firstOrNull { it.shortname == shortName }

    // Return the short names of the org itself and every level up to the consortium.
    // This is used to implement "located URIs".
    @JvmStatic
    fun getOrgAncestry(shortName: String): List<String> {
        val ancestry = mutableListOf<String>()
        var org = findOrgByShortName(shortName)
        while (org != null) {
            ancestry.add(org.shortname)
            org = findOrg(org.id)
        }
        return ancestry
    }

    @JvmStatic
    fun getOrgInfoPageUrl(id: Int): String {
        val org = findOrg(id)
        if (org == null)
            return "";
        // jump past the header stuff to the library info
        // #content-wrapper works only sometimes
        // ?#content-wrapper no better
        // /?#main-content no better
        // trying #main-content
        return Gateway.baseUrl.plus("/eg/opac/library/${org.shortname}#main-content")
    }
}