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
package net.kenstir.hemlock.android.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.Analytics.log
import net.kenstir.hemlock.android.Analytics.redactedString
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.Library
import org.evergreen_ils.utils.ui.ActivityUtils.launchURL
import org.evergreen_ils.utils.ui.AppState
import org.evergreen_ils.utils.ui.showAlert

open class AuthenticatorActivity: AccountAuthenticatorActivity() {
    val REQ_SIGNUP: Int = 1
    protected var accountManager: AccountManager? = null
    protected val scope = lifecycleScope

    private var authTokenType: String? = null
    protected var alertMessage: String? = null
    private var selectedLibrary: Library? = null
    protected var forgotPasswordButton: Button? = null
    protected var submitButton: Button? = null

    protected open fun setContentViewImpl() {
        setContentView(R.layout.activity_login)
    }

    @SuppressLint("StringFormatInvalid")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.initialize(this)
        setContentViewImpl()

        App.init(this)

        accountManager = AccountManager.get(baseContext)

        val accountName = intent.getStringExtra(ARG_ACCOUNT_NAME)
        log(TAG, "accountName=$accountName")
        authTokenType = intent.getStringExtra(ARG_AUTH_TYPE)
        if (authTokenType == null) authTokenType = Const.AUTHTOKEN_TYPE
        log(TAG, "authTokenType=$authTokenType")

        val signInText = findViewById<TextView>(R.id.account_sign_in_text)
        signInText.text = String.format(getString(R.string.ou_account_sign_in_message), AppState.getString(AppState.LIBRARY_NAME))

        // Turn off suggestions for the accountName field.  Turning them off with setInputType worked on my phone
        // whereas using android:inputType="text|textNoSuggestions" in xml did not.
        val accountNameText = findViewById<TextView>(R.id.accountName)
        accountNameText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        accountNameText.text = accountName

        submitButton = findViewById(R.id.submit)
        submitButton?.setOnClickListener { submit() }

        try {
            forgotPasswordButton = findViewById(R.id.forgot_password_button)
        } catch (_: NoSuchFieldError) {
        }
        forgotPasswordButton?.setOnClickListener {
            val url = getString(R.string.ou_library_url) + "/eg/opac/password_reset"
            launchURL(this@AuthenticatorActivity, url)
        }

        val msg = savedInstanceState?.getString(STATE_ALERT_MESSAGE)
        if (msg != null) {
            showAlert(msg)
        }
    }

    protected open fun initSelectedLibrary() {
        setLibrary(Library(getString(R.string.ou_library_url), getString(R.string.ou_library_name)))
    }

    protected open fun setLibrary(library: Library?) {
        selectedLibrary = library
        if (library == null) {
            submitButton?.setEnabled(false)
        } else {
            submitButton?.setEnabled(true)
            App.setLibrary(library)
            log(TAG, "onLibrarySelected ${library.name} ${library.url}")
        }
    }

    override fun onStart() {
        super.onStart()
        initSelectedLibrary()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (alertMessage != null) {
            outState.putString(STATE_ALERT_MESSAGE, alertMessage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        log(TAG,"onActivityResult> requestCode=$requestCode resultCode=$resultCode")
        // The sign up activity returned that the user has successfully created
        // an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK && data != null) {
            onAuthSuccess(data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun submit() {
        log(TAG, "submit>")

        val username = (findViewById<View>(R.id.accountName) as TextView).text.toString()
        val password = (findViewById<View>(R.id.accountPassword) as TextView).text.toString()

        // TODO: use coroutines instead of AsyncTask
        scope.async {
            try {
                var authtoken: String? = null
                var errorMessage = "Login failed"
                val accountType = this@AuthenticatorActivity.getString(R.string.ou_account_type)
                val data = Bundle()

                val result = App.getServiceConfig().authService.getAuthToken(username, password)
                when (result) {
                    is Result.Success -> authtoken = result.get()
                    is Result.Error -> {
                        result.exception.message?.let { errorMessage = it }
                        showAlert(errorMessage)
                        return@async
                    }
                }

                data.putString(AccountManager.KEY_ACCOUNT_NAME, username)
                data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
                data.putString(AccountManager.KEY_AUTHTOKEN, authtoken)
                data.putString(PARAM_USER_PASS, password)
                data.putString(Const.KEY_LIBRARY_NAME, selectedLibrary!!.name)
                data.putString(Const.KEY_LIBRARY_URL, selectedLibrary!!.url)

                val intent = Intent()
                intent.putExtras(data)
                onAuthSuccess(intent)
            } catch (ex: Exception) {
                showAlert(ex)
            }
        }
    }

    private fun onAuthSuccess(intent: Intent) {
        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        val accountPassword = intent.getStringExtra(PARAM_USER_PASS)
        val library_name = intent.getStringExtra(Const.KEY_LIBRARY_NAME)
        val library_url = intent.getStringExtra(Const.KEY_LIBRARY_URL)
        val account = Account(accountName, accountType)
        log(TAG, ("onAuthSuccess> accountName=" + redactedString(accountName)
                + " accountType=" + accountType
                + " accountPassword=" + redactedString(accountPassword)
                + " library_name=" + library_name
                + " library_url=" + library_url))

        //if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
        log(TAG, "onAuthSuccess> addAccountExplicitly " + redactedString(accountName))
        val authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)
        val authtokenType = authTokenType

        // Create the account on the device
        var userdata: Bundle? = null
        if (!TextUtils.isEmpty(library_url)) {
            userdata = Bundle()
            userdata.putString(Const.KEY_LIBRARY_NAME, library_name)
            userdata.putString(Const.KEY_LIBRARY_URL, library_url)
            log(TAG,
                "onAuthSuccess> userdata, name=$library_name url=$library_url")
        }
        if (accountManager!!.addAccountExplicitly(account, accountPassword, userdata)) {
            log(TAG, "onAuthSuccess> true, setAuthToken " + redactedString(authtoken))
            // Not setting the auth token will cause another call to the server
            // to authenticate the user
            accountManager!!.setAuthToken(account, authtokenType, authtoken)
        } else {
            // Probably the account already existed, in which case update the password
            log(TAG, "onAuthSuccess> false, setPassword, setUserData")
            accountManager!!.setPassword(account, accountPassword)
            accountManager!!.setUserData(account, Const.KEY_LIBRARY_NAME, library_name)
            accountManager!!.setUserData(account, Const.KEY_LIBRARY_URL, library_url)
            log(TAG,
                "onAuthSuccess> now getUserData library_url=" + accountManager!!.getUserData(account,
                    Const.KEY_LIBRARY_URL))
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        private val TAG: String = AuthenticatorActivity::class.java.simpleName

        //TODO: add package prefix to these names as indicated at https://developer.android.com/reference/android/content/Intent#putExtra(java.lang.String,%20android.os.Parcelable)
        const val ARG_ACCOUNT_TYPE: String = "ACCOUNT_TYPE"
        const val ARG_AUTH_TYPE: String = "AUTH_TYPE"
        const val ARG_ACCOUNT_NAME: String = "ACCOUNT_NAME"

        //public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
        const val KEY_ERROR_MESSAGE: String = "ERR_MSG"
        const val PARAM_USER_PASS: String = "USER_PASS"
        protected const val STATE_ALERT_MESSAGE: String = "state_dialog"
    }
}
