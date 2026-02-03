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

package org.evergreen_ils.data.model

import net.kenstir.data.model.OrgClosure
import net.kenstir.data.model.OrgHours
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.system.EgOrg
import net.kenstir.data.model.Organization
import org.evergreen_ils.Api

class EvergreenOrganization(
    override val id: Int,
    override val level: Int,
    override val name: String,
    override val shortname: String,
    override val opacVisible: Boolean,
    override val parent: Int?,
    val obj: OSRFObject,
): Organization {
    var settingsLoaded: Boolean = false
    private val orgType: OrgType? = EgOrg.findOrgType(obj.getInt("ou_type") ?: -1)
    private var addressObj: OSRFObject? = null
    val addressID: Int? = obj.getInt("mailing_address")

    override var hours: OrgHours? = null
    override var closures: List<OrgClosure> = emptyList()

    override var email: String? = obj.getString("email")
    override var phone: String? = obj.getString("phone")
    override var eresourcesUrl: String? = null
    override var eventsURL: String? = null
    override var infoURL: String? = null
    override var meetingRoomsUrl: String? = null
    override var museumPassesUrl: String? = null

    override val isConsortium: Boolean
        get() = id == CONSORTIUM_ID
    override var isPaymentAllowed: Boolean = false

    private var isNotPickupLocationSetting: Boolean? = null
    override val isPickupLocation: Boolean
        get() {
            isNotPickupLocationSetting?.let { return !it }
            orgType?.canHaveVols?.let { return it }
            return true // should not happen
        }
    override val canHaveUsers: Boolean
        get() = orgType?.canHaveUsers ?: true
    override val canHaveVols: Boolean
        get() = orgType?.canHaveVols ?: true

    override var indentedDisplayPrefix = ""

    override val navigationAddress: String?
        get() = getAddress(", ")
    override val displayAddress: String?
        get() = getAddress("\n")

    private fun getAddress(separator: String): String {
        if (addressObj == null) return ""
        return buildString {
            append(addressObj?.getString("street1"))
            addressObj?.getString("street2")?.takeIf { it.isNotEmpty() }?.let { append(" ").append(it) }
            if (isNotEmpty()) { append(separator) }
            append(addressObj?.getString("city"))
            append(", ").append(addressObj?.getString("state"))
            append(" ").append(addressObj?.getString("post_code"))
        }
    }

    fun loadSettings(obj: OSRFObject) {
        eventsURL = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_EVENTS_URL)
        eresourcesUrl = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_ERESOURCES_URL)
        meetingRoomsUrl = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_MEETING_ROOMS_URL)
        museumPassesUrl = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL)
        infoURL = obj.getStringValueFromOrgSetting(Api.SETTING_INFO_URL)
        isNotPickupLocationSetting = obj.getBooleanValueFromOrgSetting(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        isPaymentAllowed = obj.getBooleanValueFromOrgSetting(Api.SETTING_CREDIT_PAYMENTS_ALLOW) ?: false
        settingsLoaded = true
    }

    fun loadAddress(obj: OSRFObject?) {
        addressObj = obj
    }

    fun loadHours(obj: OSRFObject?) {
        this.hours = EvergreenOrgHours(obj)
    }

    fun loadClosures(objList: List<OSRFObject>) {
        closures = EvergreenOrgClosure.makeArray(objList)
    }

    companion object {
        const val CONSORTIUM_ID = 1 // // as defaulted in Open-ILS code
    }
}
