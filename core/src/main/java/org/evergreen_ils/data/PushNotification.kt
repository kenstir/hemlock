/*
 * Copyright (c) 2024 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.data

import android.os.Bundle

class PushNotification(val title: String?, val body: String?, val type: String?, val username: String?) {
    constructor(extras: Bundle) : this(null, null, extras.getString(TYPE_KEY), extras.getString(USERNAME_KEY))

    override fun toString(): String {
        return "{user=$username type=${type} title=\"$title\" body=\"$body\"}"
    }

    companion object {
        const val TYPE_KEY = "hemlock.t"

        // These TYPE_* values correspond to Android notification channel IDs;
        // once created these IDs cannot be changed.  But the names and descriptions
        // can be changed.
        // See also https://developer.android.com/develop/ui/views/notifications/channels
        const val TYPE_CHECKOUTS = "checkouts"
        const val TYPE_GENERAL = "general"
        const val TYPE_HOLDS = "holds"
        const val TYPE_PMC = "pmc"

        const val USERNAME_KEY = "hemlock.u"

        // This function ensures we use a valid channel ID for an incoming notification type.
        // Notifications sent with an unregistered type are not delivered.
        fun getChannelId(type: String?): String {
            return when (type) {
                "checkouts" -> TYPE_CHECKOUTS
                "holds" -> TYPE_HOLDS
                "general" -> TYPE_GENERAL
                "main" -> TYPE_GENERAL // Acorn is using type=main
                "pmc" -> TYPE_PMC
                else -> TYPE_PMC
            }
        }
    }
}
