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
package net.kenstir.hemlock.data.models

open class Organization(
    @JvmField val id: Int,
    @JvmField val level: Int,
    @JvmField val name: String,
    @JvmField val shortname: String,
    @JvmField val opacVisible: Boolean,
    @JvmField var parent: Int?
) {
    var email: String? = null
    var phone: String? = null
    var eresourcesUrl: String? = null
    var eventsURL: String? = null
    var infoURL: String? = null
    var meetingRoomsUrl: String? = null
    var museumPassesUrl: String? = null
    open val isConsortium: Boolean = false
    var isPaymentAllowed: Boolean = false
    open val isPickupLocation: Boolean = true
    var settingsLoaded = false

    // display fields
    var indentedDisplayPrefix = ""
    val spinnerLabel: String
        get() = indentedDisplayPrefix + name

    open fun getAddress(separator: String = " "): String {
        return ""
    }
}
