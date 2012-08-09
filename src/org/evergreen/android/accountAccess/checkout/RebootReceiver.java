package org.evergreen.android.accountAccess.checkout;

import java.util.Calendar;
import java.util.List;

import org.androwrapee.db.DefaultDAO;
import org.evergreen.android.database.DatabaseManager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class RebootReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent arg1) {
		//reinitialize notifications
		
		DefaultDAO<NotificationAlert> daoNotifications = DatabaseManager.getDAOInstance(context, NotificationAlert.class, NotificationAlert.tableName);
		daoNotifications.open();

		// Fetch all alarms
		List<NotificationAlert> alarms = daoNotifications.fetchAll("");
		
		System.out.println(" Alarms " + alarms.size());

		for(int i=0;i<alarms.size();i++){
			System.out.println("notification " + alarms.get(i));
		
			Calendar cal = Calendar.getInstance();
			cal.setTime(alarms.get(i).triggerDate);

			Intent intent = new Intent(context, NotificationReceiver.class);

			System.out.println("Set Notification with message " + alarms.get(i).message + " on time :" +  cal.getTime());
			intent.putExtra("checkoutMessage", alarms.get(i).message);
			
			// In reality, you would want to have a static variable for the

			PendingIntent sender = PendingIntent.getBroadcast(context, alarms.get(i).intent_val, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			// Get the AlarmManager service
			AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
		
		}
		
		daoNotifications.close();

	}

}
