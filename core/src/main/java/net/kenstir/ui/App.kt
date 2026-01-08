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
package net.kenstir.ui

import net.kenstir.data.model.Account
import net.kenstir.data.model.Account.Companion.noAccount

object App {
    private const val TAG = "App"

    // request/result codes for use with startActivityForResult
    const val REQUEST_MESSAGES: Int = 10002

    var account: Account = noAccount
    var fcmNotificationToken: String? = null
}
