/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
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
