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

import androidx.core.os.bundleOf
import org.evergreen_ils.Api
import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.net.GatewayEventError
import org.opensrf.util.OSRFObject
import java.text.DateFormat
import java.util.Date

private const val TAG = "Account"

data class AccountCredentials(val authToken: String, val id: Int)

class Account constructor(val username: String, var authToken: String?) {
    constructor(username: String) : this(username, null)

    var id: Int? = null
    var homeOrg: Int? = null
    var barcode: String? = null
    var expireDate: Date? = null
    var notifyByEmail = false
    var notifyByPhone = false
    var notifyBySMS = false
    var smsCarrier: Int? = null
    var smsNumber: String? = null
    var holdNotifyValue: String? = null // kept for analytics
    var circHistoryStart: String? = null
    var savedPushNotificationData: String? = null // last saved user setting, not the current token
    var savedPushNotificationEnabled: Boolean = false

    var bookBags = ArrayList<BookBag>()

    private var dayPhone: String? = null
    private var firstGivenName: String? = null
    private var familyName: String? = null
    private var notifyPhoneNumber: String? = null
    private var _pickupOrg: Int? = null
    private var _searchOrg: Int? = null

    val phoneNumber: String?
        get() = notifyPhoneNumber ?: dayPhone
    val displayName: String
        get() {
            return if (username == barcode && firstGivenName != null && familyName != null) {
                "$firstGivenName $familyName"
            } else {
                username
            }
        }
    val pickupOrg: Int?
        get() = _pickupOrg ?: homeOrg
    val searchOrg: Int?
        get() = _searchOrg ?: homeOrg
    val expireDateString: String?
        get() = expireDate?.let { DateFormat.getDateInstance().format(it) }

    /** return (authToken, userID) or throw GatewayEventError */
    fun getCredentialsOrThrow(): AccountCredentials {
        val authToken = this.authToken
        val id = this.id
        if (authToken == null || id == null) {
            throw GatewayEventError.makeNoSessionError()
        }
        return AccountCredentials(authToken, id)
    }

    fun clearAuthToken() {
        authToken = null
    }

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
        this.circHistoryStart = map[Api.USER_SETTING_CHECKOUT_HISTORY_START]
        this.savedPushNotificationData = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA]
        this.savedPushNotificationEnabled = map[Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_ENABLED] == "true"
    }

    fun loadBookBags(objects: List<OSRFObject>) {
        bookBags = BookBag.makeArray(objects)
        Analytics.logEvent(Analytics.Event.BOOKBAGS_LOAD, bundleOf(
            Analytics.Param.NUM_ITEMS to bookBags.size
        ))
    }

    fun parseHoldNotifyValue(value: String?) {
        // NB: value may be either ':' separated or '|' separated, e.g. "phone:email" or "email|sms"
        this.notifyByEmail = value?.contains("email") ?: false
        this.notifyByPhone = value?.contains("phone") ?: false
        this.notifyBySMS = value?.contains("sms") ?: false
    }

    // Fix stupid setting strings that are returned with extra quotes
    // e.g. "\"160\"" -> "160"
    fun String.removeStupidExtraQuotes(): String? {
        var s = this
        if (s.startsWith("\"")) s = s.replace("\"", "")
        return s
    }

    // authTokenOrThrow fixes the problem where Kotlin warns if you try to pass a mutable property
    // to a function that takes a non-optional
    // "Smart cast to X is impossible, because Y is a mutable property that could have been changed by this time"
    fun authTokenOrThrow(): String {
        authToken?.let { return it }
        throw Exception("No authToken")
    }

    fun idOrThrow(): Int {
        id?.let { return it }
        throw Exception("Null userID")
    }
}
