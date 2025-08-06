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

package net.kenstir.ui.pn

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.kenstir.hemlock.R
import net.kenstir.logging.Log.TAG_FCM
import net.kenstir.ui.BaseActivity

class MessagingService: FirebaseMessagingService() {

    /** Called when message is received and the app is in the foreground. */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG_FCM, "[fcm] onMessageReceived: $remoteMessage")
        val notification = PushNotification(remoteMessage.notification?.title,
            remoteMessage.notification?.body,
            remoteMessage.data[PushNotification.TYPE_KEY],
            remoteMessage.data[PushNotification.USERNAME_KEY])
        Log.d(TAG_FCM, "[fcm] foreground notification: $notification")
        sendNotification(notification)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG_FCM, "[fcm] Refreshed token: $token")
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     */
    private fun sendNotification(notification: PushNotification) {
        val requestCode = 0
        val clazz = BaseActivity.activityForNotificationType(notification)

        val intent = Intent(this, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, notification.type.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
