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

import org.opensrf.util.OSRFObject
import java.lang.Exception

class Account constructor(val username: String, var authToken: String?) {
    constructor(username: String) : this(username, null)

    var id: Int? = null
    var homeOrg: Int? = null
    var barcode: String? = null

    private var settings = mutableMapOf<String, Any?>()
    private var dayPhone: String? = null

    val defaultNotifyEmail: Boolean
        get() = false // TODO
    val defaultNotifyPhone: Boolean
        get() = false // TODO
    val defaultNotifySMS: Boolean
        get() = false // TODO
    val phoneNumber: String?
        get() = dayPhone // TODO

    fun clearSession() {
        // TODO: clear all session data
        authToken = null
    }

    fun authTokenOrThrow(): String {
        authToken?.let { return it }
        throw Exception("No authToken")
    }

    fun loadSession(obj: OSRFObject) {
        id = obj.getInt("id")
        homeOrg = obj.getInt("home_ou")
        dayPhone = obj.getString("day_phone")
    }

    fun loadUserSettings() {
    }
}
