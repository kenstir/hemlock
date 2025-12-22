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

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.app.Activity
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.kenstir.data.model.Library
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import kotlin.coroutines.resumeWithException

/**
 * Wrappers for AccountUtils functions to move AccountManager calls off the main thread.
 *
 * I'm a little hesitant to rewrite AccountUtils as kotlin, but we occasionally
 * see ANRs caused by slow Binder transactions to AccountManager.
 */
object AccountUtilsAsync {
    suspend fun addAccount(
        activity: Activity
    ): Bundle = suspendCancellableCoroutine { cont ->
        Log.d(Const.AUTH_TAG, "[auth] addAccount")
        val am = AccountManager.get(activity)
        val accountType = activity.getString(R.string.ou_account_type)
        val callback = AccountManagerCallback<Bundle> { future ->
            try {
                val result = future.result
                cont.resume(result) { cause, _, _ -> }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
        am.addAccount(accountType,
            Const.AUTHTOKEN_TYPE,
            null,
            null,
            activity,
            callback,
            null)
    }

    suspend fun getAccountsByType(
        context: Context
    ): List<Account> = withContext(Dispatchers.IO) {
        val am = AccountManager.get(context)
        val accountType: String? = context.getString(R.string.ou_account_type)
        val availableAccounts = am.getAccountsByType(accountType)
        Log.d(Const.AUTH_TAG, "[auth] getAccountsByType found ${availableAccounts.size} accounts")
        availableAccounts.toList()
    }

    /** Convenience helper: if 1 account, get an auth token; if more, show account chooser; if none, add account.
     *
     * See [AccountManager.getAuthTokenByFeatures]
     */
    suspend fun getAuthTokenHelper(
        activity: Activity
    ): Bundle = suspendCancellableCoroutine { cont ->
        Log.d(Const.AUTH_TAG, "[auth] getAuthTokenByFeatures")
        val am = AccountManager.get(activity)
        val accountType = activity.getString(R.string.ou_account_type)
        val callback = AccountManagerCallback<Bundle> { future ->
            try {
                val result = future.result
                cont.resume(result) { cause, _, _ -> }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
        am.getAuthTokenByFeatures(
            accountType,
            Const.AUTHTOKEN_TYPE,
            null,
            activity,
            null,
            null,
            callback,
            null
        )
    }

    suspend fun getAuthToken(
        activity: Activity,
        accountName: String
    ): Bundle = suspendCancellableCoroutine { cont ->
        Log.d(Const.AUTH_TAG, "[auth] getAuthTokenForAccountFuture $accountName")
        val am = AccountManager.get(activity)
        val accountType = activity.getString(R.string.ou_account_type)
        val account = Account(accountName, accountType)
        val callback = AccountManagerCallback<Bundle> { future ->
            try {
                val result = future.result
                cont.resume(result) { cause, _, _ -> }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
        am.getAuthToken(
            account,
            Const.AUTHTOKEN_TYPE,
            null,
            activity,
            callback,
            null)
    }

    suspend fun getLibraryForAccount(
        context: Context,
        accountName: String,
        accountType: String
    ): Library = withContext(Dispatchers.IO) {
        AccountUtils.getLibraryForAccount(context, accountName, accountType)
    }

    suspend fun invalidateAuthToken(
        context: Context,
        authToken: String?
    ) = withContext(Dispatchers.IO) {
        AccountUtils.invalidateAuthToken(context, authToken)
    }
}
