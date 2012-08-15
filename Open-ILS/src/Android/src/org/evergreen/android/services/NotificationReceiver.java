package org.evergreen.android.services;

import org.evergreen.android.views.splashscreen.SplashActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = "NotificationManager";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(ns);

        String checkoutMessage = intent.getStringExtra("checkoutMesage");

        Log.d(TAG, "Message " + checkoutMessage);
        // send notification

        int icon = android.R.drawable.ic_dialog_alert;
        CharSequence tickerText = "Checkout item due date";
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);

        notification.defaults |= Notification.FLAG_AUTO_CANCEL;

        CharSequence contentTitle = "EG - checkout item due date";
        CharSequence contentText = checkoutMessage;
        // start evergreen
        Intent notificationIntent = new Intent(context, SplashActivity.class);
        notificationIntent.putExtra("jump", "checkout_items");

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

}
