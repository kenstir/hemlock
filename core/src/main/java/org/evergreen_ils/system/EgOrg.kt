/*
 * Copyright (c) 2020 Kenneth H. Cox
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
package org.evergreen_ils.system

import android.annotation.SuppressLint
import androidx.core.util.Pair
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.OrgType
import org.evergreen_ils.data.Organization
import org.evergreen_ils.net.Gateway
import org.opensrf.util.OSRFObject
import java.util.*
import kotlin.Comparator

object EgOrg {
    var orgTypes = mutableListOf<OrgType>()
    private var orgs = mutableListOf<Organization>()
    var smsEnabled = false
    const val consortiumID = 1
    private const val TAG = "EgOrg"

    val allOrgs: List<Organization>
        get() = orgs
    @JvmStatic
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
        Log.d(TAG, "loadOrgTypes: ${objArray.size} org types")
    }

    @JvmStatic
    fun findOrgType(id: Int): OrgType? {
        return orgTypes.firstOrNull { it.id == id }
    }

    private fun addOrganization(obj: OSRFObject, level: Int) {
        val id = obj.getInt("id") ?: return
        val name = obj.getString("name") ?: return
        val shortName = obj.getString("shortname") ?: return
        val ouType = obj.getInt("ou_type") ?: return
        val opacVisible = obj.getBoolean("opac_visible")
        val org = Organization(id, level, name, shortName, ouType, opacVisible, obj)
        org.indentedDisplayPrefix = String(CharArray(level)).replace("\u0000", "   ")
        Log.v(TAG, "org id:${org.id} level:${org.level} vis:${org.opacVisible} shortname:${org.shortname} name:${org.name}")
        orgs.add(org)
        val children = obj.get("children") as? List<OSRFObject>
        children?.forEach { child ->
            val child_level = if (opacVisible) level + 1 else level
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
                    if (a.id == consortiumID) return@Comparator -1
                    if (b.id == consortiumID) 1 else a.name.compareTo(b.name)
                })
                for (o in orgs) {
                    o.indentedDisplayPrefix = ""
                }
            }
        }
        Log.d(TAG, "loadOrgs: ${orgs.size} orgs")
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

    // return list of spinner labels and the index at which defaultOrgId appears else (0)
    fun orgSpinnerLabelsAndSelectedIndex(defaultOrgId: Int?): Pair<ArrayList<String>, Int> {
        val labels: ArrayList<String> = ArrayList<String>(visibleOrgs.size)
        var selectedIndex = 0
        for ((index, org) in visibleOrgs.withIndex()) {
            labels.add(org.spinnerLabel)
            if (org.id == defaultOrgId) {
                selectedIndex = index
            }
        }
        return Pair(labels, selectedIndex)
    }

    @SuppressLint("DefaultLocale")
    fun dumpOrgStats() {
        val numPickupLocations = visibleOrgs.count { it.isPickupLocation }
        val numWithEvents = visibleOrgs.count { !it.eventsURL.isNullOrEmpty() }
        val numWithEresources = visibleOrgs.count { !it.eresourcesUrl.isNullOrEmpty() }
        val numWithMeetingRooms = visibleOrgs.count { !it.meetingRoomsUrl.isNullOrEmpty() }
        val numWithMuseumPasses = visibleOrgs.count { !it.museumPassesUrl.isNullOrEmpty() }
        val numRequireMonographicPart = orgs.count { it.requireMonographicPart != null }
        Log.d(TAG, String.format("%3d visible orgs", visibleOrgs.size))
        Log.d(TAG, String.format("%3d are pickup locations", numPickupLocations))
        Log.d(TAG, String.format("%3d have events URLs", numWithEvents))
        Log.d(TAG, String.format("%3d have eresources URLs", numWithEresources))
        Log.d(TAG, String.format("%3d have meeting rooms URLs", numWithMeetingRooms))
        Log.d(TAG, String.format("%3d have museum passes URLs", numWithMuseumPasses))
        Log.d(TAG, String.format("%3d have require_monographic_part set", numRequireMonographicPart))
        print("")
    }
}
