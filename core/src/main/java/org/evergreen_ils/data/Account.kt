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
import org.opensrf.util.OSRFObject

private const val TAG = "Account"

class Account constructor(val username: String, var authToken: String?) {
    constructor(username: String) : this(username, null)

    companion object {
        val emptyAccount = Account("defaultUserName")
    }

    var id: Int? = null
    var homeOrg: Int? = null
    var barcode: String? = null
    var notifyByEmail: Boolean? = null
    var notifyByPhone: Boolean? = null
    var notifyBySMS: Boolean? = null
    var smsCarrier: Int? = null
    var smsNumber: String? = null

    private var dayPhone: String? = null
    private var defaultPickupOrg: Int? = null
    private var defaultPhone: String? = null
    private var defaultSearchOrg: Int? = null
    private var defaultSMSCarrier: Int? = null

    val phoneNumber: String?
        get() = defaultPhone ?: dayPhone
    val pickupOrg: Int?
        get() = defaultPickupOrg ?: homeOrg
    val searchOrg: Int?
        get() = defaultSearchOrg ?: homeOrg

    fun clearSession() {
        authToken = null
        id = null
        homeOrg = null
        barcode = null
        notifyByEmail = null
        notifyBySMS = null
        smsCarrier = null
        smsNumber = null
        dayPhone = null
        defaultPickupOrg = null
    }

    fun loadSession(obj: OSRFObject) {
        id = obj.getInt("id")
        homeOrg = obj.getInt("home_ou")
        dayPhone = obj.getString("day_phone")
    }

    fun loadUserSettings(obj: OSRFObject) {
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
        this.defaultPickupOrg = Api.parseInt(map[Api.USER_SETTING_DEFAULT_PICKUP_LOCATION])
        this.defaultPhone = map[Api.USER_SETTING_DEFAULT_PHONE]
        this.defaultSearchOrg = Api.parseInt(map[Api.USER_SETTING_DEFAULT_SEARCH_LOCATION])
        this.defaultSMSCarrier = Api.parseInt(map[Api.USER_SETTING_DEFAULT_SMS_CARRIER])
        this.smsNumber = map[Api.USER_SETTING_DEFAULT_SMS_NOTIFY]
        parseHoldNotifyValue(map[Api.USER_SETTING_HOLD_NOTIFY])
    }

    fun parseHoldNotifyValue(value: String?) {
        // NB: value may be either ':' separated or '|' separated, e.g. "phone:email" or "email|sms"
        this.notifyByEmail = value?.contains("email")
        this.notifyByPhone = value?.contains("phone")
        this.notifyBySMS = value?.contains("sms")
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
