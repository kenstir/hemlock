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

package net.kenstir.data.model

import androidx.core.os.bundleOf
import net.kenstir.util.Analytics
import org.evergreen_ils.gateway.GatewayEventError
import java.text.DateFormat
import java.util.Date

data class AccountCredentials(val authToken: String, val id: Int)

open class Account(val username: String, var authToken: String?) {
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
    var holdHistoryStart: String? = null
    var savedPushNotificationData: String? = null // last saved user setting, not the current token
    var savedPushNotificationEnabled: Boolean = false

    var patronLists = listOf<PatronList>()

    protected var dayPhone: String? = null
    protected var firstGivenName: String? = null
    protected var familyName: String? = null
    protected var notifyPhoneNumber: String? = null
    protected var _pickupOrg: Int? = null
    protected var _searchOrg: Int? = null

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
    var pickupOrg: Int?
        get() = _pickupOrg ?: homeOrg
        set(value) {
            _pickupOrg = value
        }
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

    protected fun onListsLoaded() {
        Analytics.logEvent(Analytics.Event.BOOKBAGS_LOAD, bundleOf(
            Analytics.Param.NUM_ITEMS to patronLists.size
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
