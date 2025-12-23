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
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.kenstir.data.model.Library
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import kotlin.coroutines.resumeWithException

/**
 * Wrappers for AccountManager functions
 *
 * Fixing ANRs caused by slow Binder transactions to AccountManager.
 *
 * Strategy:
 * - calls that return Futures are wrapped in suspendCancellableCoroutine to await
 *   the single-shot callback
 * - calls which are known causes of ANRs are wrapped in withContext(Dispatchers.IO)
 * - other calls are left on the calling thread for now
 *
 * These functions are annotated with the following attributes:
 * - safe-await: uses suspendCancellableCoroutine to await a callback from AccountManager
 * - safe-io: uses withContext(Dispatchers.IO) to run on a background thread
 * - unsafe: has caused ANRs when called on the main thread
 * - unknown: not yet implicated in ANRs, may need to be revisited later
 */
object AccountUtilsKt {

    /** Calls [AccountManager.addAccount] and awaits the result
     *
     * ANR-Safety: safe-await
     */
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
        am.addAccount(
            accountType,
            Const.AUTHTOKEN_TYPE,
            null,
            null,
            activity,
            callback,
            null)
    }

    /** Convenience helper: if 1 account, get an auth token; if more, show account chooser; if none, add account.
     *
     * Calls [AccountManager.getAuthTokenByFeatures] and awaits the result
     *
     * ANR-Safety: safe-await
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

    /** Calls [AccountManager.getAuthToken] and awaits the result
     *
     * ANR-Safety: safe-await
     */
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

    /** Removes all accounts of our type from AccountManager
     *
     * ANR-Safety: safe-io
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    suspend fun removeAllAccounts(context: Context) = withContext(Dispatchers.IO) {
        val am = AccountManager.get(context)
        val accountType = context.getString(R.string.ou_account_type)
        val accounts = am.getAccountsByType(accountType)
        for (account in accounts) {
            Log.d(Const.AUTH_TAG, "[auth] removing account ${account.name}")
            val ok = am.removeAccountExplicitly(account)
            if (!ok) {
                Log.d(Const.AUTH_TAG, "[auth] failed to remove account ${account.name}")
            }
        }
    }

    /** Given an accountName, return a Library object
     *
     * This is necessary because of the way we create accounts in the generic Hemlock app,
     * using [AccountManager.getAuthTokenByFeatures], which creates the account but does not
     * tell us the URL of the selected library.
     *
     * ANR-Safety: unsafe, has caused ANRs in the past
     */
    fun getLibraryForAccountBlocking(
        context: Context,
        accountName: String,
        accountType: String
    ): Library {
        val am = AccountManager.get(context)
        val account = Account(accountName, accountType)
        Log.d(Const.AUTH_TAG, "[auth] getLibraryForAccount: accountName=$accountName")

        // For custom apps, libraryUrl and libraryName come from the resources.
        // For the generic Hemlock app, they are stored as user data in the AccountManager.
        var libraryUrl = context.getString(R.string.ou_library_url)
        Log.d(Const.AUTH_TAG, "[auth]    libraryUrl from resources: $libraryUrl")
        if (TextUtils.isEmpty(libraryUrl)) {
            libraryUrl = am.getUserData(account, Const.KEY_LIBRARY_URL)
            Log.d(Const.AUTH_TAG, "[auth]    libraryUrl from user data: $libraryUrl")
        }
        var libraryName = context.getString(R.string.ou_library_name)
        Log.d(Const.AUTH_TAG, "[auth]    libraryName from resources: $libraryName")
        if (TextUtils.isEmpty(libraryName)) {
            libraryName = am.getUserData(account, Const.KEY_LIBRARY_NAME)
            Log.d(Const.AUTH_TAG, "[auth]    libraryName from user data: $libraryName")
        }

        return Library(libraryUrl, libraryName)
    }

    /** Given an accountName, return a Library object
     *
     * This is necessary because of the way we create accounts in the generic Hemlock app,
     * using [AccountManager.getAuthTokenByFeatures], which creates the account but does not
     * tell us the URL of the selected library.
     *
     * ANR-Safety: safe-io
     */
    suspend fun getLibraryForAccount(
        context: Context,
        accountName: String,
        accountType: String
    ): Library = withContext(Dispatchers.IO) {
        getLibraryForAccountBlocking(context, accountName, accountType)
    }

    /** Calls [AccountManager.getAccountsByType]
     *
     * ANR-Safety: unknown
     */
    fun getAccountsByType(context: Context): List<Account> {
        val am = AccountManager.get(context)
        val accountType: String? = context.getString(R.string.ou_account_type)
        val availableAccounts = am.getAccountsByType(accountType)
        Log.d(Const.AUTH_TAG, "[auth] getAccountsByType found ${availableAccounts.size} accounts")
        return availableAccounts.toList()
    }

    /** Calls [AccountManager.invalidateAuthToken]
     *
     * ANR-Safety: unknown
     */
    fun invalidateAuthToken(context: Context, authToken: String?) {
        Log.d(Const.AUTH_TAG, "[auth] invalidateAuthToken $authToken")
        if (authToken.isNullOrEmpty()) return
        val am = AccountManager.get(context)
        val accountType: String? = context.getString(R.string.ou_account_type)
        am.invalidateAuthToken(accountType, authToken)
    }

    /** Calls [AccountManager.clearPassword]
     *
     * ANR-Safety: unknown
     */
    fun clearPassword(context: Context, accountName: String) {
        val am = AccountManager.get(context)
        val accountType = context.getString(R.string.ou_account_type)
        val account = Account(accountName, accountType)
        am.clearPassword(account)
    }
}
