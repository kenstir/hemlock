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
import org.evergreen_ils.system.EgOrg
import org.opensrf.util.OSRFObject

class Organization(@JvmField val id: Int,
                   @JvmField val level: Int,
                   @JvmField val name: String,
                   @JvmField val shortname: String,
                   @JvmField val ouType: Int,
                   @JvmField val opac_visible: Boolean,
                   @JvmField val obj: OSRFObject) {

    // optional fields are loaded from the aou object
    @JvmField val parent_ou = obj.getInt("parent_ou")
    @JvmField val email = obj.getString("email")
    @JvmField val phone = obj.getString("phone")

    var addressObj: OSRFObject? = null

    var indentedDisplayPrefix = ""
    var settingsLoaded = false
    private var isNotPickupLocationSetting: Boolean? = null // null=not loaded
    var isPaymentAllowedSetting: Boolean? = null // null=not loaded
    var infoURL: String? = null
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
        get() = parent_ou == null

    fun loadSettings(obj: OSRFObject) {
        infoURL = parseStringSetting(obj, Api.SETTING_INFO_URL)
        isNotPickupLocationSetting = parseBoolSetting(obj, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        isPaymentAllowedSetting = parseBoolSetting(obj, Api.SETTING_CREDIT_PAYMENTS_ALLOW)
        val smsEnable = parseBoolSetting(obj, Api.SETTING_SMS_ENABLE)
        smsEnable?.let { EgOrg.smsEnabled = smsEnable }
        settingsLoaded = true
    }

    // map returned from `fetchOrgSettings` looks like:
    // {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
    fun parseBoolSetting(obj: OSRFObject, setting: String): Boolean? {
        val valueObj = obj.getObject(setting)
        val value = valueObj?.getBoolean("value")
        return value
    }

    fun parseStringSetting(obj: OSRFObject, setting: String): String? {
        val valueObj = obj.getObject(setting)
        val value = valueObj?.getString("value")
        return value
    }

    fun getAddress(separator: String = " "): String {
        var sb = StringBuilder()
        sb.append(addressObj?.getString("street1"))
        addressObj?.getString("street2")?.let { sb.append(separator).append(it) }
        sb.append(separator).append(addressObj?.getString("city"))
        sb.append(", ").append(addressObj?.getString("state"))
        //sb.append(separator).append(addressObj?.getString("country"))
        sb.append(" ").append(addressObj?.getString("post_code"))
        return sb.toString()
    }
}
