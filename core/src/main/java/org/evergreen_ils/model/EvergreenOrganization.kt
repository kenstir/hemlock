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

package org.evergreen_ils.model

import org.evergreen_ils.xdata.XOSRFObject
import org.evergreen_ils.system.EgOrg
import net.kenstir.hemlock.data.model.Organization
import org.evergreen_ils.Api
import org.evergreen_ils.data.OrgType
import org.evergreen_ils.xdata.parseOrgBoolSetting
import org.evergreen_ils.xdata.parseOrgStringSetting

class EvergreenOrganization(
    id: Int,
    level: Int,
    name: String,
    shortname: String,
    opacVisible: Boolean,
    parent: Int?,
    val obj: XOSRFObject,
): Organization(id, level, name, shortname, opacVisible, parent) {
    private val orgType: OrgType? = EgOrg.findOrgType(obj.getInt("ou_type") ?: -1)
    val addressID: Int? = obj.getInt("mailing_address")
    private var addressObj: XOSRFObject? = null
    private var hoursObj: XOSRFObject? = null
    private var closures: List<XOSRFObject>? = null

    override val isConsortium: Boolean
        get() = id == CONSORTIUM_ID

    var isNotPickupLocationSetting: Boolean? = null
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

    override val hasAddress: Boolean
        get() = addressObj != null

    override fun getAddress(separator: String): String {
        if (addressObj == null) return ""
        val sb = StringBuilder()
        sb.append(addressObj?.getString("street1"))
        addressObj?.getString("street2")?.let { sb.append(separator).append(it) }
        sb.append(separator).append(addressObj?.getString("city"))
        sb.append(", ").append(addressObj?.getString("state"))
        //sb.append(separator).append(addressObj?.getString("country"))
        sb.append(" ").append(addressObj?.getString("post_code"))
        return sb.toString()
    }

    fun loadSettings(obj: XOSRFObject) {
        eventsURL = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_EVENTS_URL)
        eresourcesUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_ERESOURCES_URL)
        meetingRoomsUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_MEETING_ROOMS_URL)
        museumPassesUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL)
        infoURL = parseOrgStringSetting(obj, Api.SETTING_INFO_URL)
        isNotPickupLocationSetting = parseOrgBoolSetting(obj, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        isPaymentAllowed = parseOrgBoolSetting(obj, Api.SETTING_CREDIT_PAYMENTS_ALLOW) ?: false
        val smsEnable = parseOrgBoolSetting(obj, Api.SETTING_SMS_ENABLE)
        smsEnable?.let { EgOrg.smsEnabled = smsEnable }
        settingsLoaded = true
    }

    fun loadAddress(obj: XOSRFObject) {
        addressObj = obj
    }

    fun loadHours(obj: XOSRFObject?) {
        hoursObj = obj
    }

    fun loadClosures(obj: List<XOSRFObject>) {
        closures = obj
    }

    companion object {
        const val CONSORTIUM_ID = 1 // // as defaulted in Open-ILS code
    }

}
