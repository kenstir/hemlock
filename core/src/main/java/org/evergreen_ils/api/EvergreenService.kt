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

package org.evergreen_ils.api

import org.evergreen_ils.Api
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.system.CopyStatus
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.OrgType
import org.evergreen_ils.system.Organization
import org.open_ils.idl.IDLParser
import org.opensrf.util.OSRFObject
import java.util.*

// `EvergreenService` owns the state about the server: orgs, orgTypes, and IDL.
class EvergreenService {
    companion object {
        var copyStatusList = mutableListOf<CopyStatus>()
        var orgTypes = mutableListOf<OrgType>()
        var orgs = mutableListOf<Organization>()

        suspend fun loadIDL() {
            var now = System.currentTimeMillis()
            val url = Gateway.getIDLUrl()
            val xml = Gateway.fetchString(url)
            val parser = IDLParser(xml.byteInputStream())
            now = Log.logElapsedTime(Api.TAG, now, "loadIDL.get")
            parser.parse()
            Log.logElapsedTime(Api.TAG, now, "loadIDL.parse")
        }

        fun loadOrgTypes(objArray: List<OSRFObject>) {
            var newOrgTypes = mutableListOf<OrgType>()
            objArray.forEach { obj ->
                val id = obj.getInt("id")
                if (id != null) {
                    val orgType = OrgType(id,
                            obj.getString("name"),
                            obj.getString("opac_label"),
                            Api.parseBoolean(obj.getString("can_have_users")),
                            Api.parseBoolean(obj.getString("can_have_vols")))
                    newOrgTypes.add(orgType)
                }
            }
            synchronized(this) {
                orgTypes = newOrgTypes
            }
            Log.d(TAG, "loadOrgTypes: ${objArray.size} org types")
        }

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
            Log.d(TAG, "id:$id level:${org.level} vis:${org.opac_visible} shortname:${org.shortname} name:${org.name}")
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
            Log.d(TAG, "loadOrgs: ${orgs.size} orgs")
        }

        fun findOrg(id: Int): Organization? = orgs.firstOrNull { it.id == id }

        fun getOrgShortNameSafe(id: Int): String = findOrg(id)?.shortname ?: "?"

        fun getOrgNameSafe(id: Int): String = findOrg(id)?.name ?: "?"

        fun findOrgByShortName(shortName: String): Organization? = orgs.firstOrNull { it.shortname == shortName }

        // Return the short names of the org itself and every level up to the consortium.
        // This is used to implement "located URIs".
        fun getOrgAncestry(shortName: String): List<String> {
            val ancestry = mutableListOf<String>()
            var org = findOrgByShortName(shortName)
            while (org != null) {
                ancestry.add(org.shortname)
                org = findOrg(org.id)
            }
            return ancestry
        }

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

        fun loadCopyStatuses(ccs_list: List<OSRFObject>) {
            synchronized(this) {
                copyStatusList.clear()
                for (ccs_obj in ccs_list) {
                    if (Api.parseBoolean(ccs_obj.getString("opac_visible"))) {
                        val id = ccs_obj.getInt("id")
                        val name = ccs_obj.getString("name")
                        if (id != null && name != null) {
                            copyStatusList.add(CopyStatus(id, name))
                            Log.d(TAG, "loadCopyStatus id:$id name:$name")
                        }
                    }
                }
                copyStatusList.sort()
            }
        }
    }
}
