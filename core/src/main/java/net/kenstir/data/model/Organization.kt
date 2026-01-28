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
    open var isPaymentAllowed: Boolean = false
    open val isPickupLocation: Boolean = true
    open val canHaveUsers: Boolean = true
    open val canHaveVols: Boolean = true

    var settingsLoaded = false

    open var hours: OrgHours? = null
    open var closures: List<OrgClosure> = emptyList()

    // display fields
    var indentedDisplayPrefix = ""
    val spinnerLabel: String
        get() = indentedDisplayPrefix + name

    open val navigationAddress: String? = null
    open val displayAddress: String? = null
}
