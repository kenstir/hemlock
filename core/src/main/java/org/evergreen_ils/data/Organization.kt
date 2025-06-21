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

import net.kenstir.hemlock.data.evergreen.Api
import net.kenstir.hemlock.data.evergreen.XOSRFObject
import org.evergreen_ils.system.EgOrg

class Organization(@JvmField val id: Int,
                   @JvmField val level: Int,
                   @JvmField val name: String,
                   @JvmField val shortname: String,
                   @JvmField val ouType: Int,
                   @JvmField val opacVisible: Boolean,
                   @JvmField var aouObj: XOSRFObject) {

    val parent: Int?
        get() = aouObj.getInt("parent_ou")
    val addressID: Int?
        get() = aouObj.getInt("mailing_address")
    val email: String?
        get() = aouObj.getString("email")
    val phone: String?
        get() = aouObj.getString("phone")

    var addressObj: XOSRFObject? = null

    var indentedDisplayPrefix = ""
    var settingsLoaded = false
    private var isNotPickupLocationSetting: Boolean? = null // null=not loaded
    var isPaymentAllowedSetting: Boolean? = null // null=not loaded
    var eresourcesUrl: String? = null
    var eventsURL: String? = null
    var infoURL: String? = null
    var meetingRoomsUrl: String? = null
    var museumPassesUrl: String? = null
    var requireMonographicPart: Boolean? = null

    val spinnerLabel: String
        get() = indentedDisplayPrefix + name
    val isPickupLocation: Boolean
        get() {
            isNotPickupLocationSetting?.let { return !it }
            orgType?.canHaveVols?.let { return it }
            return true // should not happen
        }
    val orgType: OrgType?
        get() = EgOrg.findOrgType(ouType)
    val isConsortium: Boolean
        get() = parent == null

    fun loadSettings(obj: XOSRFObject) {
        eventsURL = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_EVENTS_URL)
        eresourcesUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_ERESOURCES_URL)
        meetingRoomsUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_MEETING_ROOMS_URL)
        museumPassesUrl = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL)
        infoURL = parseOrgStringSetting(obj, Api.SETTING_INFO_URL)
        isNotPickupLocationSetting = parseOrgBoolSetting(obj, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        isPaymentAllowedSetting = parseOrgBoolSetting(obj, Api.SETTING_CREDIT_PAYMENTS_ALLOW)
        requireMonographicPart = parseOrgBoolSetting(obj, Api.SETTING_REQUIRE_MONOGRAPHIC_PART)
        val smsEnable = parseOrgBoolSetting(obj, Api.SETTING_SMS_ENABLE)
        smsEnable?.let { EgOrg.smsEnabled = smsEnable }
        settingsLoaded = true
    }

    fun getAddress(separator: String = " "): String {
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
}

// map returned from `fetchOrgSettings` looks like:
// {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
private fun parseOrgBoolSetting(obj: XOSRFObject, setting: String): Boolean? {
    val valueObj = obj.getObject(setting)
    return valueObj?.getBoolean("value")
}

fun parseOrgStringSetting(obj: XOSRFObject, setting: String): String? {
    val valueObj = obj.getObject(setting)
    return valueObj?.getString("value")
}
