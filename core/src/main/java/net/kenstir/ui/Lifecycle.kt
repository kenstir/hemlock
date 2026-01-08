/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package net.kenstir.ui

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import net.kenstir.hemlock.R
import net.kenstir.ui.account.AuthenticatorActivity
import net.kenstir.ui.view.launch.LaunchActivity
import net.kenstir.ui.view.main.MainActivity
import net.kenstir.util.Analytics
import net.kenstir.util.Analytics.logException

/**
 * Manages app lifecycle state and restarting the app from the top.
 *
 * Android may choose to initialize the app at a non-MAIN activity if the
 * app crashed or for other reasons.  In these cases we want to force sane
 * initialization via the LaunchActivity.  The LaunchActivity ensures that
 * the service layer is initialized before starting the main Activity.
 *
 * You may say to yourself, but is not documented this way, Android should
 * not be starting at a non-MAIN activity.  In practice it happens
 */
object Lifecycle {
    private const val TAG = "Lifecycle"

    var isStarted = false

    /**
     * Restarts the app at the top (LaunchActivity)
     */
    fun restartApp(activity: Activity) {
        restartAppWithAccount(activity, null)
    }

    /**
     * Restarts the app at the top (LaunchActivity) with the specified account
     */
    fun restartAppWithAccount(activity: Activity, accountName: String?) {
        Analytics.log(TAG, "[init] restartApp $accountName")
        val intent = Intent(activity.applicationContext, LaunchActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (accountName != null)
            intent.putExtra(AuthenticatorActivity.Companion.ARG_ACCOUNT_NAME, accountName)
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * Marks the app as started and launches the main activity as the new root activity
     */
    fun startApp(activity: Activity) {
        Analytics.log(TAG, "[init] startApp")
        isStarted = true
        AppState.incrementLaunchCount()
        val intent = getMainActivityIntent(activity)
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * Starts app from a push notification, possibly with a back stack
     */
    fun startAppFromPushNotification(activity: Activity, targetActivityClass: Class<out BaseActivity?>?) {
        Analytics.log(TAG, "[init][fcm] startAppFromPushNotification")
        isStarted = true
        AppState.incrementLaunchCount()

        // Start the app with a back stack, so if the user presses Back, the app does not exit.
        val mainIntent = getMainActivityIntent(activity)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val intent = Intent(activity.applicationContext, targetActivityClass)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = TaskStackBuilder.create(activity.applicationContext)
            .addNextIntent(mainIntent)
            .addNextIntent(intent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (pendingIntent != null) {
                pendingIntent.send()
            } else {
                activity.startActivity(intent)
                activity.finish()
            }
        } catch (e: CanceledException) {
            logException(e)
        }
    }

    /**
     * Gets an intent to start the main activity, which may be MainList or MainGrid
     */
    fun getMainActivityIntent(activity: Activity): Intent {
        val clazzName: String = activity.getString(R.string.ou_main_activity)
        if (clazzName.isNotEmpty()) {
            try {
                val clazz = Class.forName(clazzName)
                return Intent(activity.applicationContext, clazz)
            } catch (e: Exception) {
                logException(e)
            }
        }
        return Intent(activity.applicationContext, MainActivity::class.java)
    }
}
