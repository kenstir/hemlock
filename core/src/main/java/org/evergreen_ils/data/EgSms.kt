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
import org.evergreen_ils.system.Log
import org.opensrf.util.OSRFObject

private const val TAG = "EgSms"

object EgSms {
    @JvmStatic
    var carriers = mutableListOf<SMSCarrier>()

    // map returned from `fetchOrgSettings` looks like:
    // {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
    fun parseBoolSetting(map: Map<String, Any?>, setting: String): Boolean? {
        var value: Boolean? = null
        if (map != null) {
            val o = map[setting]
            if (o != null) {
                val setting_map = o as Map<String, *>
                value = Api.parseBoolean(setting_map["value"])
            }
        }
        return value
    }

    fun loadCarriers(carriers: List<OSRFObject>) {
        synchronized(this) {
            this.carriers.clear()
            for (obj in carriers) {
                val id = obj.getInt("id")
                val name = obj.getString("name")
                if (id != null && name != null) {
                    this.carriers.add(SMSCarrier(id, name))
                    Log.d(TAG, "loadSMSCarriers id:$id name:$name")
                } else {
                    throw Error("wtf")
                }
            }
            this.carriers.sort()
        }
    }

    @JvmStatic
    fun findCarrier(id: Int): SMSCarrier? = carriers.firstOrNull { it.id == id }
}
