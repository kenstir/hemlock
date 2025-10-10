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

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import kotlinx.coroutines.runBlocking
import net.kenstir.hemlock.R
import net.kenstir.util.Analytics.log
import net.kenstir.util.Analytics.redactedString
import net.kenstir.ui.App
import org.evergreen_ils.gateway.GatewayClient

class AccountAuthenticator(private val context: Context): AbstractAccountAuthenticator(context) {
    private var authenticatorActivity: Class<*>? = null

    init {
        // Choose the right AuthenticatorActivity.  A custom app does not require the library spinner.
        val libraryUrl = context.getString(R.string.ou_library_url)
        if (TextUtils.isEmpty(libraryUrl)) {
            this.authenticatorActivity = GenericAuthenticatorActivity::class.java
        } else {
            this.authenticatorActivity = AuthenticatorActivity::class.java
        }
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle
    ): Bundle? {
        log(TAG, "addaccount $accountType $authTokenType")
        val intent = Intent(context, authenticatorActivity)
        // setting ARG_IS_ADDING_NEW_ACCOUNT here does not work, because this is not the
        // same Intent as the one in AuthenticatorActivity.finishLogin
        //intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

        val result = Bundle()
        result.putParcelable(AccountManager.KEY_INTENT, intent)
        return result
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle {
        log(TAG, "getAuthToken> " + redactedString(account.name))

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (authTokenType != Const.AUTHTOKEN_TYPE) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            return result
        }

        val am = AccountManager.get(context)
        var libraryName = am.getUserData(account, Const.KEY_LIBRARY_NAME)
        var libraryUrl = am.getUserData(account, Const.KEY_LIBRARY_URL)
        log(TAG, "getAuthToken> library_name=$libraryName library_url=$libraryUrl")
        if (libraryName == null) {
            // workaround issue #24 - not sure how it happened
            libraryName = context.getString(R.string.ou_library_name)
        }
        if (libraryUrl == null) {
            // workaround issue #24 - not sure how it happened
            libraryUrl = context.getString(R.string.ou_library_url)
        }

        var authToken = am.peekAuthToken(account, authTokenType)
        log(TAG, "getAuthToken> peekAuthToken returned " + redactedString(authToken))
        if (TextUtils.isEmpty(authToken)) {
            val password = am.getPassword(account)
            if (password != null) {
                try {
                    log(TAG, "getAuthToken> attempting to sign in with existing password")
                    if (libraryUrl != GatewayClient.baseUrl) {
                        // The app changed the server URL; don't send the old password to the new server.
                        // This path clears the password and prompts the user to sign in again.
                        throw AuthenticationException("Server URL changed, please sign in again")
                    }
                    authToken = runBlocking {
                        App.getServiceConfig().authService.getAuthToken(account.name, password).get()
                    }
                } catch (e: AuthenticationException) {
                    //Analytics.logException(e);
                    am.clearPassword(account)
                    val result = Bundle()
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, e.message)
                    return result
                } catch (e2: Exception) {
                    //Analytics.logException(e2);
                    am.clearPassword(account)
                    val result = Bundle()
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, "Sign in failed")
                    return result
                }
            }
        }

        // If we get an authToken - we return it
        log(TAG, "getAuthToken> token " + redactedString(authToken))
        if (!TextUtils.isEmpty(authToken)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            result.putString(Const.KEY_LIBRARY_NAME, libraryName)
            result.putString(Const.KEY_LIBRARY_URL, libraryUrl)
            return result
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        log(TAG, "getAuthToken> creating intent to display AuthenticatorActivity")
        val intent = Intent(context, authenticatorActivity)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type)
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String): String {
        return Const.AUTHTOKEN_TYPE_LABEL
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        features: Array<String>
    ): Bundle {
        log(TAG, "hasFeatures features $features")
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return result
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle? {
        log(TAG, "editProperties $accountType")
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        options: Bundle
    ): Bundle? {
        log(TAG, "confirmCredentials")
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle? {
        log(TAG, "updateCredentials")
        return null
    }

    companion object {
        private const val TAG = "AccountAuthenticator"
    }
}
