/*
 * Copyright (c) 2024 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.utils.ui

import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.Log.TAG_FCM
import org.evergreen_ils.data.PushNotification
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway

open class MainBaseActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    /// FCM: handle background push notification
    /// If launch intent is a push notification of type PMC, launch Messages activity and return true
    fun onCreateHandleLaunchIntent(): Boolean {
        Log.d(TAG_FCM, "MainActivity intent: $intent")
        intent.extras?.let {
            val notification = PushNotification(it)
            Log.d(TAG_FCM, "background notification: $notification")
            Log.d(TAG_FCM, "launch intent extras: $it")
            if (notification.type == PushNotification.TYPE_PMC) {
                // TODO: launch Messages activity
                return true
            }
        }
        return false
    }

    fun initializePushNotifications() {
        if (!resources.getBoolean(R.bool.ou_enable_push_notifications)) return

        requestNotificationPermission()
        createNotificationChannels()
        updateStoredNotificationToken()
    }

    fun updateStoredNotificationToken() {
        scope.async {
            val start = System.currentTimeMillis()

            // get the fcmToken
            val result = fetchFcmNotificationToken()
            if (result is Result.Error) {
                showAlert(result.exception)
                return@async
            }

            // If the current FCM token is different from the one we got from the user settings,
            // we need to update the user setting in Evergreen
            val storedToken = App.getAccount().storedFcmToken
            val currentToken = App.getFcmNotificationToken()
            Log.d(TAG_FCM, "stored token:  $storedToken")
            if (currentToken != null && currentToken != storedToken) {
                val updateResult = Gateway.actor.updatePushNotificationToken(App.getAccount(), currentToken)
                if (updateResult is Result.Error) {
                    showAlert(updateResult.exception)
                    return@async
                }
            }
        }
    }
}
