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

package net.kenstir.ui.account

import android.accounts.AccountManager
import android.os.Bundle

data class AccountManagerResult (
        val accountName: String?,
        val accountType: String?,
        val authToken: String?,
        val errorMessage: String?
) {
    val failed: Boolean
        get() = accountName.isNullOrEmpty() || authToken.isNullOrEmpty()
    val failureMessage: String
        get() = errorMessage ?: "Login failed"
}

fun Bundle.getAccountManagerResult(): AccountManagerResult {
    return AccountManagerResult(
        getString(AccountManager.KEY_ACCOUNT_NAME),
        getString(AccountManager.KEY_ACCOUNT_TYPE),
        getString(AccountManager.KEY_AUTHTOKEN),
        getString(AccountManager.KEY_ERROR_MESSAGE)
    )
}
