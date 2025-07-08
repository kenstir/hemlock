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

package net.kenstir.ui.pn

import android.os.Bundle

// The `channelId` string is used when registering the notification channels.
// Once created, a channelId string cannot be changed without uninstalling the app.
// The descriptions can be changed, e.g. R.string.notification_channel_checkouts_name.
// See also https://developer.android.com/develop/ui/views/notifications/channels
//
// NB: This list of channelId strings must be kept in sync in 3 places:
// * hemlock (android): core/src/main/java/org/evergreen_ils/data/PushNotification.kt
// * hemlock-ios:       Source/Models/PushNotification.swift
// * hemlock-sendmsg:   sendmsg.go
enum class NotificationType(val channelId: String) {
    CHECKOUTS("checkouts"),
    FINES("fines"),
    GENERAL("general"),
    HOLDS("holds"),
    PMC("pmc");

    companion object {
        fun make(strValue: String?): NotificationType {
            return values().find { it.channelId == strValue } ?: HOLDS
        }
    }
}

class PushNotification(val title: String?, val body: String?, val type: NotificationType, val username: String?) {
    constructor(title: String?, body: String?, type: String?, username: String?) :
            this(title, body, NotificationType.make(type), username)
    constructor(extras: Bundle) :
            this(null, null, extras.getString(TYPE_KEY), extras.getString(USERNAME_KEY))

    fun isNotGeneral(): Boolean {
        return type != NotificationType.GENERAL
    }

    override fun toString(): String {
        return "{user=$username chan=${type.channelId} title=\"$title\" body=\"$body\"}"
    }

    companion object {
        const val TYPE_KEY = "hemlock.t"
        const val USERNAME_KEY = "hemlock.u"
    }
}
