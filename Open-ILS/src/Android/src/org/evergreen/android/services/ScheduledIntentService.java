package org.evergreen.android.services;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.androwrapee.db.DefaultDAO;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.accountAccess.checkout.CircRecord;
import org.evergreen.android.database.DatabaseManager;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.open_ils.idl.IDLParser;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScheduledIntentService extends IntentService{

	public static Date lastUpdateServiceDate;
	
	public static String TAG = "ScheduledIntentService";
	
	public static String ACTION = "org.evergreen.updateservice";
	
	//fire up once a day
	public static int SCHEDULE_TIME_INTERVAL = 1;
	
	public ScheduledIntentService(){
		super("EvergreenIntentService");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Start service");
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		 Date currentDate = new Date(System.currentTimeMillis());
		
		 Log.d(TAG, "Notifications service started");
		 
		 /* Download the necessary IDL files for checkout items operations like au (for auth), circ, mvr and acp
		  */
		 String idlFile = GlobalConfigs.IDL_FILE_FROM_ROOT + "?class=circ&class=au&class=mvr&class=acp";
	   	 try{
	   		Log.d("debug","Read fm");
	   		InputStream in_IDL = Utils.getNetInputStream(GlobalConfigs.httpAddress + idlFile);
	   		IDLParser parser = new IDLParser(in_IDL);
	   		parser.parse();
	   	}catch(Exception e){
	   		System.err.println("Error in parsing IDL file " + idlFile + " " + e.getMessage());
	   	};
	   	
	   	//login with the user credentials
	   	AccountAccess accountAccess = AccountAccess.getAccountAccess(GlobalConfigs.httpAddress, (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));
	   	boolean auth = true;
	   	try {
			accountAccess.authenticate();
		} catch (NoNetworkAccessException e) {
			auth=false;
			e.printStackTrace();
		} catch (NoAccessToServer e) {
			auth = false;
			e.printStackTrace();
		}
		
	   	//if we managed to authenticate we start
	   	if(auth){
	   		
	   		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	   		//int daysBeforeDueDate = sharedPreferences.getInt("notifications_days_before_expiration", 2);
	   		//TODO
	   		int daysBeforeDueDate = 2;
	   		ArrayList<CircRecord> circRecords = new ArrayList<CircRecord>();
	   		
	   		//get the circ records
			try {
				circRecords = accountAccess.getItemsCheckedOut();
			} catch (NoNetworkAccessException e) {
				//not suppose to happen
			} catch (NoAccessToServer e) {
				//not suppose to happen
			} catch (SessionNotFoundException e) {
				//auth just earlier realized, not supose to happen
			}
			
			DefaultDAO<NotificationAlert> daoNotifications = DatabaseManager.getDAOInstance(this, NotificationAlert.class, NotificationAlert.tableName);
			daoNotifications.open();

			// Fetch all alarms from database
			List<NotificationAlert> alarms = daoNotifications.fetchAll("");

			for(int i=0;i<alarms.size();i++){
				System.out.println("notification " + alarms.get(i));
				Log.d(TAG, "Notification " + alarms.get(i));
			}
			for (int i = 0; i < circRecords.size(); i++) {

				CircRecord checkoutRecord = circRecords.get(i);

				Date dueDate = checkoutRecord.getDueDateObject();
		
				Calendar notificationDate = Calendar.getInstance();
				notificationDate.setTime(dueDate);
				
				
				notificationDate.add(Calendar.DAY_OF_MONTH, -daysBeforeDueDate);
				Log.d(TAG, " notification time start "+notificationDate.getTime() + " current date " + currentDate  + " date between " + currentDate);
				
				
				// if due date in the future
				if (currentDate.compareTo(notificationDate.getTime()) >= 0) {

					// get a Calendar object with current time
					Calendar cal = Calendar.getInstance();
					
					cal.setTime(dueDate);

					//just for test
					cal.add(Calendar.HOUR, 4);
					cal.add(Calendar.MINUTE, 37);
					
					Log.d(TAG, "Set notification in " + cal.getTime());
					
					NotificationAlert notifications = daoNotifications.fetch(checkoutRecord.circ_id);
					NotificationAlert newNotificationInf = new NotificationAlert(checkoutRecord.circ_id, NotificationAlert.NOTIFICATION_INTENT
							+ checkoutRecord.circ_id, cal.getTime(), "Checkout " + checkoutRecord.getAuthor() + " expires on " + checkoutRecord.getDueDate());
					
					if(notifications == null){
						daoNotifications.insert(newNotificationInf, false);
					}
					else{
						//update info in database
						daoNotifications.update(newNotificationInf, checkoutRecord.circ_id);
					}
					
					Intent intentNotification = new Intent(this, NotificationReceiver.class);

					Log.d(TAG, "Set due date alarm at" + cal.getTime() + " for " + newNotificationInf.id + " intent_val: "+ newNotificationInf.intent_val);
					
					intentNotification.putExtra("checkoutMessage", "The item " + checkoutRecord.getAuthor() + " is about to expire on " +checkoutRecord.getDueDate() );
					// update the current intent if it exists
					PendingIntent sender = PendingIntent.getBroadcast(this,
							NotificationAlert.NOTIFICATION_INTENT + checkoutRecord.circ_id, intentNotification,
							PendingIntent.FLAG_UPDATE_CURRENT);

					// Get the AlarmManager service
					AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
					am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
				}
			}
			daoNotifications.close();

			
			lastUpdateServiceDate = currentDate;
	        SharedPreferences.Editor editor = sharedPreferences.edit();
	        editor.putLong("lastUpdateTime", lastUpdateServiceDate.getTime());
	        editor.commit();
	        
			Log.d(TAG, "set last service update date " + lastUpdateServiceDate);
	   	}
	   	
	}

}
