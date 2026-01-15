/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package org.evergreen_ils.util

import net.kenstir.data.model.Account
import org.evergreen_ils.gateway.GatewayEventException

data class AccountCredentials(val authToken: String, val id: Int)

/** return (authToken, userID) or throw GatewayEventError */
fun Account.getCredentialsOrThrow(): AccountCredentials {
    val authToken = this.authToken
    val id = this.id
    if (authToken == null || id == null) {
        throw GatewayEventException.makeNoSessionError()
    }
    return AccountCredentials(authToken, id)
}
