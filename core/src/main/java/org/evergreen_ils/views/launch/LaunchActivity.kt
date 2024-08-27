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

package org.evergreen_ils.views.launch

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.evergreen_ils.R
import org.evergreen_ils.android.AccountUtils
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.Log.TAG_FCM
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.PushNotification
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.await
import org.evergreen_ils.utils.getAccountManagerResult
import org.evergreen_ils.utils.getCustomMessage
import org.evergreen_ils.utils.ui.AppState
import org.evergreen_ils.utils.ui.BaseActivity.Companion.activityForNotificationType
import org.opensrf.util.OSRFObject
import java.util.concurrent.TimeoutException

class LaunchActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName

    private var mProgressText: TextView? = null
    private var mProgressBar: View? = null
    private var mRetryButton: Button? = null
    private var mSendReportButton: Button? = null
    private lateinit var mModel: LaunchViewModel
    private var mRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Analytics.initialize(this)
        App.init(this)

        mProgressText = findViewById(R.id.action_in_progress)
        mProgressBar = findViewById(R.id.activity_splash_progress_bar)
        mRetryButton = findViewById(R.id.activity_splash_retry_button)
        mSendReportButton = findViewById(R.id.activity_splash_log_button)

        mProgressBar?.visibility = View.INVISIBLE
        mRetryButton?.visibility = View.GONE
        mSendReportButton?.visibility = View.GONE

        mRetryButton?.setOnClickListener {
            ++mRetryCount
            launchLoginFlow()
        }
        mSendReportButton?.setOnClickListener {
            sendErrorReport()
        }

        mModel = ViewModelProvider(this)[LaunchViewModel::class.java]
        mModel.status.observe(this, Observer { s ->
            Log.d(TAG, "status:$s")
            mProgressText?.text = s
        })
        mModel.spinner.observe(this, Observer { show ->
            Log.d(TAG, "spinner:$show")
            mProgressBar?.visibility = if (show) View.VISIBLE else View.INVISIBLE
        })
        mModel.serviceDataReady.observe(this, Observer { ready ->
            Log.d(TAG, "serviceDataReady:$ready")
            if (ready) {
                loadAccountData()
            } else {
                onLaunchFailure()
            }
        })
    }

    override fun onDestroy() {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onAttachedToWindow()

        launchLoginFlow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onConfigurationChanged(newConfig)
    }

    private fun onLaunchFailure() {
        mRetryButton?.visibility = View.VISIBLE
        if (mRetryCount > 0) {
            mSendReportButton?.visibility = View.VISIBLE
        }
        mProgressBar?.visibility = View.INVISIBLE
    }

    private fun onLaunchSuccess() {
        Log.d(TAG, (object{}.javaClass.enclosingMethod?.name ?: "") + " isFinishing:$isFinishing")

        // FCM: handle background push notification
        Log.d(TAG_FCM, "[onLaunchSuccess] LaunchActivity intent: $intent")
        intent.extras?.let {
            val notification = PushNotification(it)
            Log.d(TAG_FCM, "[onLaunchSuccess] notification: $notification")
            if (notification.isNotGeneral()) {
                val targetActivityClass = activityForNotificationType(notification)
                App.startAppFromPushNotification(this, targetActivityClass)
                return
            }
        }

        App.startApp(this)
    }

    private fun launchLoginFlow() {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        // Use GlobalScope so this coroutine isn't canceled when the AuthenticatorActivity is
        // started and this activity is destroyed.  Use Dispatchers.Main so that if an exception
        // happens, it is safe to update the UI.
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val account = getAccount()
                Log.d(TAG, "[auth] ${account.username} ${account.authToken}")
                mModel.loadServiceData(resources)
            } catch (ex: Exception) {
                Log.d(TAG, "[auth] caught", ex)
                mProgressText?.text = ex.getCustomMessage()
                onLaunchFailure()
            }
        }
    }

    private fun sendErrorReport() {
        val i = Intent(Intent.ACTION_SENDTO)
        i.data = Uri.parse("mailto:") // only email apps should handle this
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.ou_developer_email)))
        val appInfo = App.getAppInfo(this)
        i.putExtra(Intent.EXTRA_SUBJECT, "[Hemlock] error report - $appInfo")
        //TODO: append as attachment
        i.putExtra(Intent.EXTRA_TEXT, Analytics.getLogBuffer())
        if (i.resolveActivity(packageManager) != null) {
            startActivity(i)
        } else {
            Toast.makeText(this, "There is no email app installed", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAccountData() {
        lifecycleScope.launch {
            try {
                if (getSession(App.getAccount())) {
                    onLaunchSuccess()
                } else {
                    onLaunchFailure()
                }
            } catch (ex: Exception) {
                Analytics.logFailedLogin(ex)
                val msg = ex.message ?: "Cancelled"
                mProgressText?.text = msg
                onLaunchFailure()
            }
        }
    }

    // Here we use the AccountManager to get an auth token, maybe creating or selecting an
    // account along the way.  We have to do that here and not in a ViewModel because it
    // needs an Activity.
    private suspend fun getAccount(): Account {
        // get auth token
        val future = AccountUtils.getAuthTokenFuture(this)
        Log.d(TAG, "[auth] getAuthTokenFuture ...")
        val bnd = future.await(3_600_000) // long to allow authenticator activity
        Log.d(TAG, "[auth] getAuthTokenFuture ... $bnd")
        if (bnd == null)
            throw TimeoutException()
        val result = bnd.getAccountManagerResult()
        if (result.accountName.isNullOrEmpty() || result.authToken.isNullOrEmpty())
            throw Exception(result.failureMessage)

        // turn that into a Library and Account
        val accountType: String = applicationContext.getString(R.string.ou_account_type)
        val library = AccountUtils.getLibraryForAccount(applicationContext, result.accountName, accountType)
        AppState.setString(AppState.LIBRARY_NAME, library.name)
        AppState.setString(AppState.LIBRARY_URL, library.url)
        App.setLibrary(library)
        val account = Account(result.accountName, result.authToken)
        App.setAccount(account)
        return account
    }

    // Again we have to do this here and not in a ViewModel because it needs an Activity.
    private suspend fun getSession(account: Account): Boolean {
        mProgressText?.text = "Starting session"

        // authToken zen: try it once and if it fails, invalidate it and try again
        var sessionResult = fetchSession(account.authTokenOrThrow())
        Log.d(TAG, "[auth] sessionResult.succeeded:${sessionResult.succeeded}")
        if (sessionResult is Result.Error) {
            AccountUtils.invalidateAuthToken(this, account.authToken)
            account.authToken = null
            Log.d(TAG, "[auth] getAuthTokenForAccountFuture ...")
            val future = AccountUtils.getAuthTokenForAccountFuture(this, account.username)
            Log.d(TAG, "[auth] getAuthTokenForAccountFuture ... await")
            val bnd = future.await(3_600_000) // long to allow authenticator activity
            Log.d(TAG, "[auth] getAuthTokenForAccountFuture ... $bnd")
            val accountManagerResult = bnd.getAccountManagerResult()
            if (accountManagerResult.accountName.isNullOrEmpty() || accountManagerResult.authToken.isNullOrEmpty())
                throw Exception(accountManagerResult.failureMessage)
            account.authToken = accountManagerResult.authToken
            sessionResult = fetchSession(account.authTokenOrThrow())
            Log.d(TAG, "[auth] sessionResult.succeeded:${sessionResult.succeeded}")
        }
        when (sessionResult) {
            is Result.Success -> account.loadSession(sessionResult.data)
            is Result.Error -> {
                throw sessionResult.exception
            }
        }

        //mProgressText?.text = "Loading account preferences"

        // get user settings
        val fleshedUserResult = Gateway.actor.fetchFleshedUser(account)
        when (fleshedUserResult) {
            is Result.Success ->
                account.loadFleshedUserSettings(fleshedUserResult.data)
            is Result.Error -> {
                throw fleshedUserResult.exception
            }
        }

        // load the home org settings, used to control visibility of the Events button
        if (resources.getBoolean(R.bool.ou_enable_events_button)) {
            EgOrg.findOrg(App.getAccount().homeOrg)?.let { org ->
                val orgSettingsResult = Gateway.actor.fetchOrgSettings(org.id)
                if (orgSettingsResult is Result.Success) {
                    org.loadSettings(orgSettingsResult.data)
                    Log.v(TAG, "org ${org.id} settings loaded")
                }
            }
        }

        if (resources.getBoolean(R.bool.ou_is_generic_app)) {
            // For Hemlock, we only care to track the user's consortium
            Analytics.logSuccessfulLogin(account.username, account.barcode,
                null, EgOrg.getOrgShortNameSafe(EgOrg.consortiumID))
        } else {
            Analytics.logSuccessfulLogin(account.username, account.barcode,
                EgOrg.getOrgShortNameSafe(account.homeOrg),
                EgOrg.getOrgShortNameSafe(EgOrg.findOrg(account.homeOrg)?.parent))
        }

        return true
    }

    private suspend fun fetchSession(authToken: String): Result<OSRFObject> {
        Log.d(TAG, "[auth] fetchSession ...")
        val result = Gateway.auth.fetchSession(authToken)
        Log.d(TAG, "[auth] fetchSession ... $result")
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Analytics.log(TAG, "onactivityresult: $requestCode $resultCode")
    }
}
