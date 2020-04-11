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

class Organization(@JvmField val id: Int,
                   @JvmField val level: Int,
                   @JvmField val name: String,
                   @JvmField val shortname: String,
                   @JvmField val parent_ou: Int?,
                   @JvmField val ouType: Int,
                   @JvmField val opac_visible: Boolean) {

    var indentedDisplayPrefix = ""
    var settingsLoaded = false
    private var isNotPickupLocationSetting: Boolean? = null // null=not loaded
    var isPaymentAllowedSetting: Boolean? = null // null=not loaded
    val treeDisplayName: String
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

    fun loadSettings(map: JSONDictionary) {
        isNotPickupLocationSetting = parseBoolSetting(map, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB)
        isPaymentAllowedSetting = parseBoolSetting(map, Api.SETTING_CREDIT_PAYMENTS_ALLOW)
        val smsEnable = parseBoolSetting(map, Api.SETTING_SMS_ENABLE)
        smsEnable?.let { EgOrg.smsEnabled = smsEnable }
        settingsLoaded = true
    }

    // map returned from `fetchOrgSettings` looks like:
    // {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
    fun parseBoolSetting(map: JSONDictionary, setting: String): Boolean? {
        var value: Boolean? = null
        if (map != null) {
            val o = map[setting]
            if (o != null) {
                val setting_map = o as Map<String, Any?>
                value = Api.parseBoolean(setting_map["value"])
            }
        }
        return value
    }
}
