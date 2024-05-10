package org.evergreen_ils.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.evergreen_ils.R
import org.evergreen_ils.android.Log.TAG_FCM
import org.evergreen_ils.data.PushNotification
import org.evergreen_ils.views.MainActivity
import org.evergreen_ils.views.messages.MessagesActivity

class MessagingService: FirebaseMessagingService() {

    /** Called when message is received and the app is in the foreground. */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notification = PushNotification(remoteMessage.notification?.title,
            remoteMessage.notification?.body,
            remoteMessage.data[PushNotification.TYPE_KEY],
            remoteMessage.data[PushNotification.USERNAME_KEY])
        Log.d(TAG_FCM, "foreground notification: $notification")
        sendNotification(notification)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG_FCM, "Refreshed token: $token")
        // TODO: send token to EG server
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     */
    private fun sendNotification(notification: PushNotification) {
        val requestCode = 0
        val clazz = if (notification.type == PushNotification.TYPE_PMC) MessagesActivity::class.java else MainActivity::class.java
        val intent = Intent(this, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_library_24)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /* I think this was already done in createNotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }
        */

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
