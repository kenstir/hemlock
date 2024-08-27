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

// Once created, the channelId string cannot be changed without uninstalling the app.
// But the descriptions can be changed, e.g. R.string.notification_channel_checkouts_name.
// See also https://developer.android.com/develop/ui/views/notifications/channels
enum class HemlockNotificationChannel(val id: String) {
    CHECKOUTS("checkouts"),
    FINES("fines"),
    GENERAL("general"),
    HOLDS("holds"),
    PMC("pmc");

    companion object {
        fun fromType(notificationType: String?): HemlockNotificationChannel? {
            return values().find { it.id == notificationType }
        }
    }
}

class PushNotification(val title: String?, val body: String?, val channel: HemlockNotificationChannel, val username: String?) {
    constructor(title: String?, body: String?, type: String?, username: String?) :
            this(title, body, HemlockNotificationChannel.fromType(type) ?: HemlockNotificationChannel.HOLDS, username)
    constructor(extras: Bundle) :
            this(null, null, extras.getString(TYPE_KEY), extras.getString(USERNAME_KEY))

    fun isNotGeneral(): Boolean {
        return channel != HemlockNotificationChannel.GENERAL
    }

    override fun toString(): String {
        return "{user=$username chan=${channel.id} title=\"$title\" body=\"$body\"}"
    }

    companion object {
        const val TYPE_KEY = "hemlock.t"
        const val USERNAME_KEY = "hemlock.u"
    }
}
