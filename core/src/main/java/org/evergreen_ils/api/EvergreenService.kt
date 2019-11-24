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
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.OrgType
import org.evergreen_ils.system.Organization
import org.open_ils.idl.IDLParser
import org.opensrf.util.OSRFObject

// `EvergreenService` owns the state about the server: orgs, orgTypes, and IDL.
class EvergreenService {
    companion object {
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
    }
}
