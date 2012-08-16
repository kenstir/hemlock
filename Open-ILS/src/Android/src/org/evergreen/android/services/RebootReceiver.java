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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class RebootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        // reinitialize notifications

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        if (sharedPreferences.getBoolean("notifications_enabled", false)) {

            Toast.makeText(context, "Set up notification updates",
                    Toast.LENGTH_SHORT).show();
            // if enabled register the update service to run once per day
            // get a Calendar object with current time
            Calendar cal = Calendar.getInstance();

            Intent bRecvIntent = new Intent(context,
                    PeriodicServiceBroadcastReceiver.class);
            bRecvIntent.setAction(ScheduledIntentService.ACTION);
            // update the current intent if it exists
            PendingIntent sender = PendingIntent.getBroadcast(context,
                    NotificationAlert.NOTIFICATION_INTENT
                            + PeriodicServiceBroadcastReceiver.INTENT_ID,
                    bRecvIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the AlarmManager service
            AlarmManager am = (AlarmManager) context
                    .getSystemService(Activity.ALARM_SERVICE);
            am.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(),
                    10000 * ScheduledIntentService.SCHEDULE_TIME_INTERVAL,
                    sender);
        }

    }

}
