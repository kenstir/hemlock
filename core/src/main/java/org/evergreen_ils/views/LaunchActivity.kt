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

import android.accounts.AccountManager
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.os.OperationCanceledException
import android.os.PersistableBundle
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
import org.evergreen_ils.accountAccess.AccountUtils.getAuthTokenFuture
import org.evergreen_ils.android.App
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.Log
import org.evergreen_ils.utils.ui.ThemeManager

private const val TAG = "LaunchActivity"

class LaunchActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private var mProgressText: TextView? = null
    private var mProgressBar: View? = null
    private var mRetryButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        VolleyWrangler.init(this)
        Analytics.initialize(this)
        App.init(this)
        ThemeManager.applyNightMode()

        mProgressText = findViewById(R.id.action_in_progress)
        mProgressBar = findViewById(R.id.activity_splash_progress_bar)
        mRetryButton = findViewById(R.id.activity_splash_retry_button)

        mProgressBar?.visibility = View.GONE
        mRetryButton?.visibility = View.GONE

        mRetryButton?.setOnClickListener {
            launchLoginFlow()
        }

        val model = ViewModelProviders.of(this)[LaunchViewModel::class.java]
        model.status.observe(this, Observer { s ->
            Log.d(TAG, "status:$s")
            mProgressText?.text = s
        })
        model.spinner.observe(this, Observer { value ->
            value?.let { show ->
                mProgressBar?.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        //launchLoginFlow()
    }

    fun launchLoginFlow() {
        Log.d(TAG, "auth: launch")
        launch {
            mProgressBar?.visibility = View.VISIBLE
            mProgressText?.text = "Signing in"
            try {
                val auth_token = fetchAuthToken()
                Log.d(TAG, "auth: token:$auth_token")
                mProgressText?.text = auth_token
//                model.fetchData(auth_token)
            } catch (ex: Exception) {
                var msg = ex.message
                if (msg.isNullOrEmpty()) msg = "Cancelled"
                mProgressText?.text = msg
                mRetryButton?.visibility = View.VISIBLE
            } finally {
                mProgressBar?.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        // Start launching here in onAttachedToWindow, because setDefaultNightMode causes onCreate
        // to be called twice (and therefore we would launch/cancel/launch coroutines).
        launchLoginFlow()
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
        cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Analytics.log(TAG, "onactivityresult: $requestCode $resultCode")
    }

    suspend fun fetchAuthToken(): String {
        Log.d(TAG, "auth: getAuthTokenFuture")
        val future = getAuthTokenFuture(this)
        Log.d(TAG, "auth: wait ...")
        val bnd: Bundle? = future.await()
        Log.d(TAG, "auth: wait ... done")
        val auth_token = bnd?.getString(AccountManager.KEY_AUTHTOKEN)
        val account_name = bnd?.getString(AccountManager.KEY_ACCOUNT_NAME)
        var error_msg = bnd?.getString(AccountManager.KEY_ERROR_MESSAGE)
        if (auth_token.isNullOrEmpty() || account_name.isNullOrEmpty()) {
            if (error_msg.isNullOrEmpty()) error_msg = "Login failed"
            Analytics.log(TAG, "auth: error_msg:$error_msg")
            throw Exception(error_msg)
        }
        return auth_token
    }
}
