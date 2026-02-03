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
package org.evergreen_ils.system

import android.annotation.SuppressLint
import androidx.core.util.Pair
import net.kenstir.logging.Log
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.data.model.OrgType
import net.kenstir.data.model.Organization
import org.evergreen_ils.data.model.EvergreenOrganization
import java.util.*
import kotlin.Comparator

object EgOrg {
    private const val TAG = "EgOrg"
    const val CONSORTIUM_ID = 1

    var orgTypes = mutableListOf<OrgType>()
    private var orgs = mutableListOf<Organization>()
    var smsEnabled = false
    var alertBannerEnabled = false
    var alertBannerText: String? = null

    val allOrgs: List<Organization>
        get() = orgs
    val visibleOrgs: List<Organization>
        get() = orgs.filter { it.opacVisible }

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
        Log.d(TAG, "[orgs] ${objArray.size} org types")
    }

    fun findOrgType(id: Int): OrgType? {
        return orgTypes.firstOrNull { it.id == id }
    }

    private fun addOrganization(obj: OSRFObject, level: Int) {
        val id = obj.getInt("id") ?: return
        val name = obj.getString("name") ?: return
        val shortName = obj.getString("shortname") ?: return
        val opacVisible = obj.getBoolean("opac_visible")
        val parent = obj.getInt("parent_ou")
        val org = EvergreenOrganization(id, level, name, shortName, opacVisible, parent, obj)
        org.indentedDisplayPrefix = String(CharArray(level)).replace("\u0000", "   ")
        Log.v(TAG, "[orgs] id:${org.id} level:${org.level} vis:${org.opacVisible} shortname:${org.shortname} name:${org.name}")
        orgs.add(org)
        val children = obj.getObjectList("children")
        children?.forEach { child ->
            val childLevel = if (opacVisible) level + 1 else level
            addOrganization(child, childLevel)
        }
    }

    fun loadOrgs(orgTree: OSRFObject, useHierarchicalOrgTree: Boolean) {
        synchronized(this) {
            orgs.clear()
            addOrganization(orgTree, 0)
            // If the org tree is too big, then an indented list is unwieldy.
            // Convert it into a flat list sorted by org.name.
            if (!useHierarchicalOrgTree && orgs.size > 25) {
                Collections.sort(orgs, Comparator<Organization> { a, b ->
                    // top-level OU appears first
                    if (a.id == CONSORTIUM_ID) return@Comparator -1
                    if (b.id == CONSORTIUM_ID) 1 else a.name.compareTo(b.name)
                })
                for (o in orgs) {
                    o.indentedDisplayPrefix = ""
                }
            }
        }
        Log.d(TAG, "[orgs] ${orgs.size} orgs")
    }

    @JvmStatic
    fun findOrg(id: Int?): Organization? = orgs.firstOrNull { it.id == id }

    @JvmStatic
    fun getOrgShortNameSafe(id: Int?): String = findOrg(id)?.shortname ?: "?"

    @JvmStatic
    fun getOrgNameSafe(id: Int?): String = findOrg(id)?.name ?: "?"

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
            org = findOrg(org.parent)
        }
        return ancestry
    }

    fun orgSpinnerLabels(): List<String> {
        return visibleOrgs.map { it.spinnerLabel }
    }

    fun spinnerShortNames(): List<String> {
        return visibleOrgs.map { it.shortname }
    }

    @SuppressLint("DefaultLocale")
    fun dumpOrgStats() {
        val numPickupLocations = visibleOrgs.count { it.isPickupLocation }
        val numWithEvents = visibleOrgs.count { !it.eventsURL.isNullOrEmpty() }
        val numWithEresources = visibleOrgs.count { !it.eresourcesUrl.isNullOrEmpty() }
        val numWithMeetingRooms = visibleOrgs.count { !it.meetingRoomsUrl.isNullOrEmpty() }
        val numWithMuseumPasses = visibleOrgs.count { !it.museumPassesUrl.isNullOrEmpty() }
        Log.d(TAG, String.format("[orgs] %3d visible orgs", visibleOrgs.size))
        Log.d(TAG, String.format("[orgs] %3d are pickup locations", numPickupLocations))
        Log.d(TAG, String.format("[orgs] %3d have events URLs", numWithEvents))
        Log.d(TAG, String.format("[orgs] %3d have eresources URLs", numWithEresources))
        Log.d(TAG, String.format("[orgs] %3d have meeting rooms URLs", numWithMeetingRooms))
        Log.d(TAG, String.format("[orgs] %3d have museum passes URLs", numWithMuseumPasses))

//        print("")
//        for (org in allOrgs) {
//            Log.d(TAG, String.format("%d,%s,%s", org.id, org.shortname, org.name))
//        }
//        print("")
    }
}
