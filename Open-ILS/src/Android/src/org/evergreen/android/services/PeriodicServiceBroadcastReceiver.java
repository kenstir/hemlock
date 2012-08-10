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
				lastUpdateTime
						.setTimeInMillis(prefs.getLong("lastUpdateTime", 0));

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
