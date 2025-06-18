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

package net.kenstir.hemlock.data.evergreen

import net.kenstir.hemlock.data.models.Account
import org.opensrf.util.OSRFObject

class EvergreenAccount(username: String, authToken: String?) : Account(username, authToken) {
    fun loadSession(obj: XOSRFObject) {
        id = obj.getInt("id")
        homeOrg = obj.getInt("home_ou")
        dayPhone = obj.getString("day_phone")
        firstGivenName = obj.getString("pref_first_given_name") ?: obj.getString("first_given_name")
        familyName = obj.getString("pref_family_name") ?: obj.getString("family_name")
        expireDate = obj.getDate("expire_date")
    }

    fun loadFleshedUserSettings(obj: XOSRFObject) {
        barcode = obj.getObject("card")?.getString("barcode")

        // settings is a list of objects with name and value as string;
        // construct a map of all settings, then parse out the ones we care about
        val settings = obj.get("settings") as? List<OSRFObject>
        val map = mutableMapOf<String, String>()
        settings?.forEach {
            val name = it.getString("name")
            val value = it.getString("value")?.removeStupidExtraQuotes()
            if (name != null && value != null) {
                map[name] = value
            }
        }
        this._pickupOrg = OSRFUtils.parseInt(map[Api.USER_SETTING_DEFAULT_PICKUP_LOCATION])
        this.notifyPhoneNumber = map[Api.USER_SETTING_DEFAULT_PHONE]
        this._searchOrg = OSRFUtils.parseInt(map[Api.USER_SETTING_DEFAULT_SEARCH_LOCATION])
        this.smsCarrier = OSRFUtils.parseInt(map[Api.USER_SETTING_DEFAULT_SMS_CARRIER])
        this.smsNumber = map[Api.USER_SETTING_DEFAULT_SMS_NOTIFY]
        this.holdNotifyValue = map[Api.USER_SETTING_HOLD_NOTIFY] ?: "email:phone"
        parseHoldNotifyValue(holdNotifyValue)
        this.circHistoryStart = map[Api.USER_SETTING_CIRC_HISTORY_START]
        this.savedPushNotificationData = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA]
        this.savedPushNotificationEnabled = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_ENABLED] == "true"
    }
}
