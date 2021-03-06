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

package org.evergreen_ils.net

import org.open_ils.Event

open class GatewayError(message: String?): Exception(message) {
    constructor(ex: Exception): this(ex.message)

    fun isSessionExpired(): Boolean {
        (this as? GatewayEventError)?.let {
            return it.ev.textCode == "NO_SESSION"
        }
        return false
    }
}

class GatewayEventError constructor(var ev: Event): GatewayError(ev.message) {
    companion object {
        // for use when we have null/empty creds and we want to treat it like isSessionExpired()
        fun makeNoSessionError(): GatewayEventError {
            return GatewayEventError(Event(mapOf("desc" to "No session", "textcode" to "NO_SESSION")))
        }
    }
}
