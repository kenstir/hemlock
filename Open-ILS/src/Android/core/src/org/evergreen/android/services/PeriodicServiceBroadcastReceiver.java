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

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class PeriodicServiceBroadcastReceiver extends BroadcastReceiver {

    public static final int INTENT_ID = 123;

    @Override
    public void onReceive(Context context, Intent intent) {

        // do update logic
        boolean mustDoUpdate = false;

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        // determine if to use alerts or not
        boolean use_notifications = prefs.getBoolean("notifications_enabled",
                false);

        if (use_notifications) {
            // if no updates have been made
            Calendar current = Calendar.getInstance();
            Calendar lastUpdateTime = Calendar.getInstance();
            lastUpdateTime.setTimeInMillis(prefs.getLong("lastUpdateTime", 0));

            lastUpdateTime.add(Calendar.DAY_OF_MONTH,
                    ScheduledIntentService.SCHEDULE_TIME_INTERVAL);

            // if the last update time + elapsed scheduled time < current
            // time we must do an update
            if (lastUpdateTime.compareTo(current) == -1) {
                mustDoUpdate = true;
            }

            Log.d("app", "Network connectivity change or alarm must do update "
                    + mustDoUpdate);

            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            // if internet available
            if (networkInfo != null && networkInfo.isConnected()) {

                if (mustDoUpdate == true) {
                    Intent intentService = new Intent(context,
                            ScheduledIntentService.class);
                    context.startService(intentService);
                }
            }

        }

    }

}
