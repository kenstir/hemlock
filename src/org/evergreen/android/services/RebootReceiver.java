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
