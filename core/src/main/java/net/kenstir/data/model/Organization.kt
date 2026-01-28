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

interface Organization {
    val id: Int
    val level: Int
    val name: String
    val shortname: String
    val opacVisible: Boolean
    val parent: Int?

    var hours: OrgHours?
    var closures: List<OrgClosure>

    var email: String?
    var phone: String?
    val navigationAddress: String?
    val displayAddress: String?
    var eresourcesUrl: String?
    var eventsURL: String?
    var infoURL: String?
    var meetingRoomsUrl: String?
    var museumPassesUrl: String?

    val isConsortium: Boolean
    var isPaymentAllowed: Boolean
    val isPickupLocation: Boolean
    val canHaveUsers: Boolean
    val canHaveVols: Boolean

    var indentedDisplayPrefix: String
    val spinnerLabel: String
        get() = indentedDisplayPrefix + name
}
