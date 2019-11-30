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

package org.evergreen_ils.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountUtils
import org.evergreen_ils.accountAccess.AccountUtils.getAuthTokenFuture
import org.evergreen_ils.android.App
import org.evergreen_ils.api.ActorService
import org.evergreen_ils.api.AuthService
import org.evergreen_ils.data.Account
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.Log
import org.evergreen_ils.utils.await
import org.evergreen_ils.utils.getAccountManagerResult
import org.evergreen_ils.utils.ui.AppState
import org.evergreen_ils.utils.ui.ThemeManager
import org.opensrf.util.OSRFObject
import java.util.concurrent.TimeoutException

private const val TAG = "LaunchActivity"

class LaunchActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private var mProgressText: TextView? = null
    private var mProgressBar: View? = null
    private var mRetryButton: Button? = null
    private lateinit var mModel: LaunchViewModel
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Analytics.initialize(this)
        App.init(this)
        ThemeManager.applyNightMode()

        mProgressText = findViewById(R.id.action_in_progress)
        mProgressBar = findViewById(R.id.activity_splash_progress_bar)
        mRetryButton = findViewById(R.id.activity_splash_retry_button)

        mProgressBar?.visibility = View.INVISIBLE
        mRetryButton?.visibility = View.GONE

        mRetryButton?.setOnClickListener {
            launchLoginFlow()
        }

        mModel = ViewModelProviders.of(this)[LaunchViewModel::class.java]
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

        //launchLoginFlow()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
        cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        // setDefaultNightMode causes onCreate to be called twice.  Calling launchLoginFlow here
        // saves a launch/cancel cycle.
        launchLoginFlow()
    }

    private fun onLaunchFailure() {
        mRetryButton?.visibility = View.VISIBLE
        mProgressBar?.visibility = View.INVISIBLE
    }

    private fun onLaunchSuccess() {
        App.startApp(this)
    }

    private fun launchLoginFlow() {
        Log.d(TAG, "[auth] launch")
        launch {
            try {
                val account = getAccount()
                Log.d(TAG, "[auth] ${account.username} ${account.authToken}")
                mModel?.loadServiceData(resources)
            } catch (ex: Exception) {
                Log.d(TAG, "[auth] caught in launchLoginFlow", ex)
                var msg = ex.message ?: "Cancelled"
                mProgressText?.text = msg
                onLaunchFailure()
            }
        }
    }

    private fun loadAccountData() {
        launch {
            try {
                getSession(App.getAccount())
                onLaunchSuccess()
            } catch (ex: Exception) {
                Log.d(TAG, "caught in loadAccountData", ex)
                var msg = ex.message ?: "Cancelled"
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
        Log.d(TAG, "[auth] getAuthTokenFuture")
        val future = getAuthTokenFuture(this)
        Log.d(TAG, "[auth] getAuthTokenFuture ...")
        val bnd: Bundle? = future.await()
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
        // authToken zen: try it once and if it fails, invalidate it and try again
        var sessionResult = runCatching { fetchSession(account.authTokenOrThrow()) }
        var obj = sessionResult.getOrNull()
        if (obj == null) {
            AccountUtils.invalidateAuthToken(this, account.authToken)
            account.authToken = null
            Log.d(TAG, "[auth] getAuthTokenForAccountFuture ...")
            val future = AccountUtils.getAuthTokenForAccountFuture(this, account.username)
            val bnd = future.await()
            Log.d(TAG, "[auth] getAuthTokenForAccountFuture ... $bnd")
            val accountManagerResult = bnd.getAccountManagerResult()
            if (accountManagerResult.accountName.isNullOrEmpty() || accountManagerResult.authToken.isNullOrEmpty())
                throw Exception(accountManagerResult.failureMessage)
            account.authToken = accountManagerResult.authToken
            obj = fetchSession(account.authTokenOrThrow())
        }
        account.loadSession(obj)

        // get user settings
        obj = ActorService.fetchFleshedUser(account.authTokenOrThrow(), account.idOrThrow())
        account.loadFleshedUserSettings(obj)

//        Analytics.logEvent("Account: Retrieve Session",
//                "home_org", EvergreenService.Companion.getOrgShortNameSafe(homeLibraryID),
//                "pickup_org", EvergreenService.Companion.getOrgShortNameSafe(defaultPickupLibraryID),
//                "search_org", EvergreenService.Companion.getOrgShortNameSafe(defaultSearchLibraryID),
//                "hold_notify", safeString(holdNotifySetting));

        return true
    }

    private suspend fun fetchSession(authToken: String): OSRFObject {
        Log.d(TAG, "[auth] fetchSession ...")
        val obj = AuthService.fetchSession(authToken)
        Log.d(TAG, "[auth] fetchSession ... $obj")
        return obj
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Analytics.log(TAG, "onactivityresult: $requestCode $resultCode")
    }
}
