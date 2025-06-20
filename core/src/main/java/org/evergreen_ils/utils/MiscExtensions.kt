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

package org.evergreen_ils.utils

import android.accounts.AccountManager
import android.os.Bundle
import org.evergreen_ils.data.AccountManagerResult

fun Bundle.getAccountManagerResult(): AccountManagerResult {
    return AccountManagerResult(getString(AccountManager.KEY_ACCOUNT_NAME),
            getString(AccountManager.KEY_AUTHTOKEN),
            getString(AccountManager.KEY_ERROR_MESSAGE))
}

/** custom message for particular exceptional conditions
 * Not all exception messages are suitable as is for the general public
 */
fun Exception.getCustomMessage(): String {
    when (this) {
        is java.util.concurrent.TimeoutException -> return "Timeout"
        is com.android.volley.TimeoutError ->
            return "Operation timed out"
        is com.android.volley.ClientError -> {
            this.networkResponse?.statusCode.let {
                return when (it) {
                    404 -> "Not found.  The server may be down for maintenance."
                    null -> "Unknown client error.  The server may be offline."
                    else -> "Error $it.  The server may be offline."
                }
            }
        }
    }
    this.message?.let { if (it.isNotEmpty()) return it }
    return "Cancelled"
}
