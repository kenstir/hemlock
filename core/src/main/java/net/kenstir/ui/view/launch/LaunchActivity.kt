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

package net.kenstir.ui.view.launch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.kenstir.hemlock.R
import net.kenstir.util.Analytics
import net.kenstir.logging.Log
import net.kenstir.logging.Log.TAG_FCM
import net.kenstir.data.model.Account
import net.kenstir.ui.pn.PushNotification
import net.kenstir.data.Result
import net.kenstir.ui.App
import net.kenstir.ui.AppState
import org.evergreen_ils.system.EgOrg
import net.kenstir.ui.account.getAccountManagerResult
import net.kenstir.util.getCustomMessage
import net.kenstir.ui.BaseActivity.Companion.activityForNotificationType
import net.kenstir.ui.account.AccountUtilsAsync
import net.kenstir.ui.account.awaitResult
import net.kenstir.ui.util.compatEnableEdgeToEdge

class LaunchActivity : AppCompatActivity() {

    private var mProgressText: TextView? = null
    private var mProgressBar: View? = null
    private var mRetryButton: Button? = null
    private var mSendReportButton: Button? = null
    private lateinit var mModel: LaunchViewModel
    private var mRetryCount = 0

    /** Set up the window insets listener to adjust padding for system bars */
    fun adjustPaddingForEdgeToEdge() {
        //val rootLayout = findViewById<View>(R.id.root_layout)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            rootLayout?.updatePadding(top = sysBars.top, bottom = sysBars.bottom)

            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onCreate(savedInstanceState)

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        adjustPaddingForEdgeToEdge()

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
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        // FCM: handle background push notification
        intent.extras?.let {
            val notification = PushNotification(it)
            Log.d(TAG_FCM, "[fcm] notification: $notification")
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
                //injectRandomFailure("launchLoginFlow", 25)
                getAccount()
                mModel.loadServiceData(this@LaunchActivity)
            } catch (ex: Exception) {
                mProgressText?.text = ex.getCustomMessage()
                Analytics.logFailedLaunch(ex, "launchLoginFlow")
                onLaunchFailure()
            }
        }
    }

    private fun sendErrorReport() {
        lifecycleScope.async {
            try {
                // ignore failure getting the IP address
                val result = App.getServiceConfig().loaderService.fetchPublicIpAddress()
                val ip = when (result) {
                    is Result.Success -> result.get()
                    is Result.Error -> "unknown"
                }
                Analytics.logMessageToBuffer("IP $ip")

                val i = Intent(Intent.ACTION_SENDTO)
                i.data = Uri.parse("mailto:") // only email apps should handle this
                i.putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.ou_developer_email)))
                val appInfo = App.getAppInfo(this@LaunchActivity)
                i.putExtra(Intent.EXTRA_SUBJECT, "[Hemlock] error report - $appInfo")
                //TODO: append as attachment
                i.putExtra(Intent.EXTRA_TEXT, Analytics.getLogBuffer())

                // Starting with Android 11 (API level 30), you should just catch ActivityNotFoundException;
                // calling resolveActivity requires permission.
                // https://developer.android.com/training/package-visibility/use-cases
                startActivity(i)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this@LaunchActivity, "There is no email app installed", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(this@LaunchActivity, "Error: $ex", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadAccountData() {
        lifecycleScope.launch {
            try {
                //injectRandomFailure("loadAccountData", 85)
                getSession(App.getAccount())
                onLaunchSuccess()
            } catch (ex: Exception) {
                val msg = ex.message ?: "Cancelled"
                mProgressText?.text = msg
                Analytics.logFailedLaunch(ex, "loadAccountData")
                onLaunchFailure()
            }
        }
    }

    // Here we use the AccountManager to get an auth token, maybe creating or selecting an
    // account along the way.  We have to do that here and not in a ViewModel because it
    // needs an Activity.
    private suspend fun getAccount() {
        // get auth token
        Log.d(TAG, "[auth] getAuthTokenConvenienceHelper ...")
        val future = AccountUtilsAsync.getAuthTokenConvenienceHelper(this)
        Log.d(TAG, "[auth] getAuthTokenConvenienceHelper ... await")
        val bnd = future.awaitResult()
        Log.d(TAG, "[auth] getAuthTokenConvenienceHelper ... $bnd")
        val result = bnd.getAccountManagerResult()
        if (result.accountName.isNullOrEmpty() || result.authToken.isNullOrEmpty())
            throw Exception(result.failureMessage)

        // turn that into a Library and Account
        val accountType: String = applicationContext.getString(R.string.ou_account_type)
        val library = AccountUtilsAsync.getLibraryForAccount(applicationContext, result.accountName, accountType)
        AppState.setString(AppState.LIBRARY_NAME, library.name)
        App.setLibrary(library)
        val account = App.getServiceConfig().userService.makeAccount(result.accountName, result.authToken)
        App.setAccount(account)
    }

    // Again we have to do this here and not in a ViewModel because it needs an Activity.
    private suspend fun getSession(account: Account) {
        mProgressText?.text = "Starting session"

        // authToken zen: try it once and if it fails, invalidate it and try again
        var sessionResult = fetchSession(account)
        Log.d(TAG, "[auth] sessionResult.succeeded:${sessionResult.succeeded}")
        if (sessionResult is Result.Error) {
            AccountUtilsAsync.invalidateAuthToken(this, account.authToken)
            account.authToken = null
            Log.d(TAG, "[auth] getAuthToken ...")
            val future = AccountUtilsAsync.getAuthToken(this, account.username)
            Log.d(TAG, "[auth] getAuthToken ... await")
            val bnd = future.awaitResult()
            Log.d(TAG, "[auth] getAuthToken ... $bnd")
            val accountManagerResult = bnd.getAccountManagerResult()
            if (accountManagerResult.accountName.isNullOrEmpty() || accountManagerResult.authToken.isNullOrEmpty())
                throw Exception(accountManagerResult.failureMessage)
            account.authToken = accountManagerResult.authToken
            sessionResult = fetchSession(account)
            Log.d(TAG, "[auth] sessionResult.succeeded:${sessionResult.succeeded}")
        }
        when (sessionResult) {
            is Result.Success -> {
            }
            is Result.Error -> {
                throw sessionResult.exception
            }
        }

        // load the home org settings, used to control visibility of Events and other buttons
        EgOrg.findOrg(App.getAccount().homeOrg)?.let { org ->
            val result = App.getServiceConfig().orgService.loadOrgSettings(org.id)
            if (result is Result.Error) {
                throw result.exception
            }
        }

        // record analytics
        val numAccounts = AccountUtilsAsync.getAccountsByType(this).size
        if (resources.getBoolean(R.bool.ou_is_generic_app)) {
            // For Hemlock, we only care to track the user's consortium
            Analytics.logSuccessfulLaunch(account.username, account.barcode,
                null, EgOrg.getOrgShortNameSafe(EgOrg.consortiumID), numAccounts)
        } else {
            Analytics.logSuccessfulLaunch(account.username, account.barcode,
                EgOrg.getOrgShortNameSafe(account.homeOrg),
                EgOrg.getOrgShortNameSafe(EgOrg.findOrg(account.homeOrg)?.parent), numAccounts)
        }
    }

    private suspend fun fetchSession(account: Account): Result<Unit> {
        return App.getServiceConfig().userService.loadUserSession(account)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Analytics.log(TAG, "onactivityresult: $requestCode $resultCode")
    }

    companion object {
        private const val TAG = "LaunchActivity"
    }
}
