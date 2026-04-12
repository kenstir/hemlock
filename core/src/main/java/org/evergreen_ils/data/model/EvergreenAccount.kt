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

import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.gateway.OSRFObject
import net.kenstir.data.model.Account
import net.kenstir.logging.Log
import org.evergreen_ils.Api
import kotlin.io.encoding.Base64

class EvergreenAccount(username: String, authToken: String? = null): Account(username, authToken) {

    fun loadSession(obj: OSRFObject) {
        id = obj.getInt("id")
        homeOrg = obj.getInt("home_ou")
        dayPhone = obj.getString("day_phone")
        firstGivenName = obj.getString("pref_first_given_name") ?: obj.getString("first_given_name")
        familyName = obj.getString("pref_family_name") ?: obj.getString("family_name")
        expireDate = obj.getDate("expire_date")
    }

    fun loadFleshedUserSettings(obj: OSRFObject) {
        barcode = obj.getObject("card")?.getString("barcode")

        // settings is a list of objects with name and value as string;
        // construct a map of all settings, then parse out the ones we care about
        val settings = obj.getObjectList("settings")
        val map = mutableMapOf<String, String>()
        settings?.forEach {
            val name = it.getString("name")
            val value = it.getString("value")
            //Log.d(TAG, "[setting] $name: $value")
            if (name != null && value != null) {
                map[name] = removeExtraQuotesFromSettingValue(value)
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
        this.holdHistoryStart = map[Api.USER_SETTING_HOLD_HISTORY_START]
        this.savedPushNotificationData = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA]
        this.savedPushNotificationEnabled = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_ENABLED] == "true"
        Log.d(TAG, "[fcm] savedPushNotificationData: ${savedPushNotificationData?.length} bytes")
    }

    // user setting values have quotes around them sometimes, e.g. the integer 1 here:
    // {"__c":"aus","__p":[8,"opac.default_search_location",150,"\"1\""]}
    fun removeExtraQuotesFromSettingValue(value: String): String {
        return if (value.startsWith("\"") && value.endsWith("\"")) {
            //Log.d(TAG, "[setting] value has extra quotes, removing")
            value.removePrefix("\"").removeSuffix("\"")
        } else {
            value
        }
    }

    fun parseHoldNotifyValue(value: String?) {
        // NB: value may be either ':' separated or '|' separated, e.g. "phone:email" or "email|sms"
        this.notifyByEmail = value?.contains("email") ?: false
        this.notifyByPhone = value?.contains("phone") ?: false
        this.notifyBySMS = value?.contains("sms") ?: false
    }

    fun loadLists(bags: List<OSRFObject>) {
        patronLists = BookBag.makeArray(bags)
        onListsLoaded()
    }

    companion object {
        private const val TAG = "Account"
    }
}
