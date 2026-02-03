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

package net.kenstir.ui.view.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.kenstir.data.Result
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.logging.Log.TAG_FCM
import net.kenstir.logging.Log.TAG_PERM
import net.kenstir.ui.App
import net.kenstir.ui.AppState
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.account.AccountUtils
import net.kenstir.ui.pn.NotificationType
import net.kenstir.ui.util.showAlert
import net.kenstir.util.md5

/**
 * Behavior common to MainActivity and MainGridActivity.
 *
 * This primarily involves handling push notifications.
 */
open class MainBaseActivity : BaseActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG_PERM, "Permission granted (1)")
            resetDenyCount()
//        } else {
//            //TODO: change to an alert
//            //TODO: change title to "Notice" and add text on how to update it later
//            val msg = getString(R.string.notification_permission_denied_msg, getString(R.string.ou_app_label))
//            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /// FCM: handle background push notification
    // NB: this doesn't happen here, it happens in LaunchActivity.onLaunchSuccess
//    fun onCreateHandleLaunchIntent(): Boolean {
//        Log.d(TAG_FCM, "************************************** MainActivity intent: $intent")
//        intent.extras?.let {
//            val notification = PushNotification(it)
//            Log.d(TAG_FCM, "======================================================================================== background notification: $notification")
//            showAlert("background notification in MainBaseActivity: $notification")
//            if (notification.isNotGeneral()) {
//                val targetActivityClass = activityForNotificationType(notification)
//                val intent = Intent(applicationContext, targetActivityClass)
//                startActivity(intent)
//                finish()
//                return true
//            }
//        }
//        return false
//    }

    fun initializePushNotifications() {
        if (!resources.getBoolean(R.bool.ou_enable_push_notifications)) return

        requestNotificationPermission()
        createNotificationChannels()
        updateStoredNotificationToken()
    }

    // This logic is a little different from that recommended in the docs at
    // https://developer.android.com/training/permissions/requesting
    // because ActivityCompat.shouldShowRequestPermissionRationale() kept returning true even after
    // the user said "no thanks".  We avoid that case by keeping a deny counter.
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG_PERM, "API Version < 33 did not have a permission dialog")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG_PERM, "Permission granted (2)")
            resetDenyCount()
            return
        }

        // See if the system thinks we should show the rationale dialog but show it only once.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            val denyCount = getDenyCount()
            Log.d(TAG_PERM, "shouldShowRequestPermissionRationale true, denyCount = $denyCount")
            if (denyCount > 0) {
                return
            }
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.notification_permission_rationale_title, getString(R.string.ou_app_label)))
                .setMessage(getString(R.string.notification_permission_rationale_msg))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    Log.d(TAG_PERM, "Launching request for permission (2)")
                    resetDenyCount()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(getString(R.string.no_thanks)) { _, _ ->
                    Log.d(TAG_PERM, "Permission denied")
                    incrementDenyCount()
                }
                .show()
            return
        }

        // Directly ask for the permission
        Log.d(TAG_PERM, "Launching request for permission (1)")
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun resetDenyCount() {
        Log.d(TAG_PERM, "Resetting deny count")
        AppState.setInt(AppState.NOTIFICATIONS_DENY_COUNT, 0)
    }

    private fun incrementDenyCount() {
        val count = AppState.getInt(AppState.NOTIFICATIONS_DENY_COUNT) + 1
        Log.d(TAG_PERM, "Incrementing deny count to $count")
        AppState.setInt(AppState.NOTIFICATIONS_DENY_COUNT, count)
    }

    private fun getDenyCount(): Int {
        val count = AppState.getInt(AppState.NOTIFICATIONS_DENY_COUNT)
        return count
    }

    suspend fun fetchFcmNotificationToken(): Result<Unit> {
        val task = FirebaseMessaging.getInstance().token
        task.await()
        if (!task.isSuccessful) {
            return Result.Error(task.exception ?: Exception("Failed fetching notification token"))
        }
        val token = task.result
        Log.d(TAG_FCM, "[fcm] fetched token=$token")
        App.fcmNotificationToken = token
        return Result.Success(Unit)
    }

    /** Create channels to show notifications.
     * See also: https://developer.android.com/develop/ui/views/notifications/channels
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // NB: The order these are created is the order they appear in Settings>>Notifications
            if (resources.getBoolean(R.bool.notification_channel_register_holds_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.HOLDS.channelId,
                    getString(R.string.notification_channel_holds_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_checkouts_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.CHECKOUTS.channelId,
                    getString(R.string.notification_channel_checkouts_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_fines_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.FINES.channelId,
                    getString(R.string.notification_channel_fines_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_pmc_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.PMC.channelId,
                    getString(R.string.notification_channel_pmc_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            // register the spillover channel last
            if (resources.getBoolean(R.bool.notification_channel_register_general_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.GENERAL.channelId,
                    getString(R.string.notification_channel_general_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun updateStoredNotificationToken() {
        scope.async {
            // get the fcmToken
            val result = fetchFcmNotificationToken()
            if (result is Result.Error) {
                showAlert(result.exception)
                return@async
            }

            // If the current FCM token is different from the one we got from the user settings,
            // we need to update the user setting in Evergreen
            val storedToken = App.account.savedPushNotificationData
            val storedEnabledFlag = App.account.savedPushNotificationEnabled
            val currentToken = App.fcmNotificationToken
            Log.d(TAG_FCM, "[fcm] stored token was: $storedToken")
            if ((currentToken != null && currentToken != storedToken) || !storedEnabledFlag)
            {
                Log.d(TAG_FCM, "[fcm] updating stored token")
                val updateResult = App.svc.user.updatePushNotificationToken(
                    App.account, currentToken)
                if (updateResult is Result.Error) {
                    showAlert(updateResult.exception)
                    return@async
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_switch_account)
        scope.launch {
            val numAccounts = AccountUtils.getAccountsByType(this@MainBaseActivity).size
            item?.isEnabled = (numAccounts > 1)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (menuItemHandler?.onItemSelected(this, id, "main_option_menu") == true)
            return true
        return if (handleMenuAction(id)) true else super.onOptionsItemSelected(item)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object {}.javaClass.enclosingMethod?.name ?: "")
        maybeShowSystemAlert()
    }

    fun maybeShowSystemAlert() {
        // do this only the first time the main activity is attached
        if (++attachCount != 1) return
        Log.d(TAG, "[alert] attachCount=${attachCount}")

        // do nothing if there's no alert banner, or if the user squelched it by
        // tapping "don't show again" on this exact message (as determined by MD5())
        val alertBanner = App.svc.consortium.alertBanner ?: return
        val squelchedMD5 = AppState.getString(AppState.ALERT_BANNER_SQUELCHED_MD5)
        val alertBannerMD5 = alertBanner.md5()
        if (squelchedMD5 == alertBannerMD5) {
            Log.d(TAG, "[alert] already squelched")
            return
        }

        // show the alert dialog with simple HTML rendering of the message
        val spannedMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(alertBanner, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(alertBanner)
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.system_alert_title))
            .setMessage(spannedMessage)
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.system_alert_dont_show_again)) { dialog, _ ->
                AppState.setString(AppState.ALERT_BANNER_SQUELCHED_MD5, alertBannerMD5)
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()

        // set the movementMethod on the TextView to make any links clickable
        val messageView: TextView? = dialog.findViewById(android.R.id.message)
        messageView?.movementMethod = LinkMovementMethod.getInstance()
    }

    companion object {
        const val TAG = "MainBase"

        var attachCount = 0
    }
}
