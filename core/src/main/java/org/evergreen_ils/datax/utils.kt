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

package org.evergreen_ils.datax

// map returned from `fetchOrgSettings` looks like:
// {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
fun parseOrgBoolSetting(obj: XOSRFObject, setting: String): Boolean? {
    val valueObj = obj.getObject(setting)
    return valueObj?.getBoolean("value")
}

fun parseOrgStringSetting(obj: XOSRFObject, setting: String): String? {
    val valueObj = obj.getObject(setting)
    return valueObj?.getString("value")
}
