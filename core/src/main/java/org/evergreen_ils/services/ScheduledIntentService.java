/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 *
 */
package org.evergreen_ils.services;

import java.util.Date;

import net.kenstir.hemlock.logging.Log;

import android.app.IntentService;
import android.content.Intent;

public class ScheduledIntentService extends IntentService {

    public static Date lastUpdateServiceDate;

    public static String TAG = ScheduledIntentService.class.getSimpleName();

    public static String ACTION = "org.evergreen_ils.updateservice";

    // fire up once a day
    public static int SCHEDULE_TIME_INTERVAL = 1;

    public ScheduledIntentService() {
        super("EvergreenIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start service");

        return super.onStartCommand(intent, flags, startId);
    }

    //TODO: share some of this code with LoadingTask.doInBackground
    //TODO: this code needs major work.  I never saw it work so I don't
    // feel bad about disabling it for now.
    @Override
    protected void onHandleIntent(Intent intent) {

        Date currentDate = new Date(System.currentTimeMillis());

        Log.d(TAG, "Notifications service started");

        /*
         * Download the necessary IDL files for checkout items operations like
         * au (for auth), circ, mvr and acp
         */
        /*
        try {
            Log.d(TAG, "Load IDL start");
            InputStream in_IDL = Utils.getNetInputStream(GlobalConfigs.getIDLUrl());
            IDLParser parser = new IDLParser(in_IDL);
            parser.setKeepIDLObjects(false);
            parser.parse();
        } catch (Exception e) {
            Log.w("Error in parsing IDL", e);
        }
        */

        /*
        AccountAccess accountAccess = AccountAccess.getInstance(GlobalConfigs.httpAddress);
        Log.d(TAG, tag+"Signing in");
        AccountManager accountManager = AccountManager.get(this);

        // what needs to be done is
        // 1. get last used account name
        // 2. see if the account still exists
        // 3. get an auth token for it
        // 4. start an evergreen session
        AccountManagerFuture<Bundle> future = accountManager.getAuthTokenByFeatures(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, mCallingActivity, null, null, null, null);
        Bundle bnd = future.getResult();
        String auth_token = bnd.getString(AccountManager.KEY_AUTHTOKEN);
        String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
        Log.d(TAG, tag+"account_name="+account_name+" token="+auth_token);

        boolean auth = false;
        if (auth) {

            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            // int daysBeforeDueDate =
            // sharedPreferences.getInt("notifications_days_before_expiration",
            // 2);
            // TODO
            int daysBeforeDueDate = 2;
            ArrayList<CircRecord> circRecords = new ArrayList<CircRecord>();

            // get the circ records
            try {
                circRecords = accountAccess.getItemsCheckedOut();
            } catch (SessionNotFoundException e) {
                // auth just earlier realized, not supose to happen
            }

            DefaultDAO<NotificationAlert> daoNotifications = DatabaseManager
                    .getDAOInstance(this, NotificationAlert.class,
                            NotificationAlert.tableName);
            daoNotifications.open();

            // Fetch all alarms from database
            List<NotificationAlert> alarms = daoNotifications.fetchAll("");

            for (int i = 0; i < alarms.size(); i++) {
                Log.d(TAG, "Notification " + alarms.get(i));
            }
            for (int i = 0; i < circRecords.size(); i++) {

                CircRecord checkoutRecord = circRecords.get(i);

                Date dueDate = checkoutRecord.getDueDate();

                Calendar notificationDate = Calendar.getInstance();
                notificationDate.setTime(dueDate);

                notificationDate.add(Calendar.DAY_OF_MONTH, -daysBeforeDueDate);
                Log.d(TAG,
                        " notification time start "
                                + notificationDate.getTime() + " current date "
                                + currentDate + " date between " + currentDate);

                // if due date in the future
                if (currentDate.compareTo(notificationDate.getTime()) >= 0) {

                    // get a Calendar object with current time
                    Calendar cal = Calendar.getInstance();

                    cal.setTime(dueDate);

                    // just for test
                    cal.add(Calendar.HOUR, 4);
                    cal.add(Calendar.MINUTE, 37);

                    Log.d(TAG, "Set notification in " + cal.getTime());

                    NotificationAlert notifications = daoNotifications
                            .fetch(checkoutRecord.circId);
                    NotificationAlert newNotificationInf = new NotificationAlert(
                            checkoutRecord.circId,
                            NotificationAlert.NOTIFICATION_INTENT
                                    + checkoutRecord.circId, cal.getTime(),
                            "Checkout " + checkoutRecord.getAuthor()
                                    + " expires on "
                                    + checkoutRecord.getDueDateString());

                    if (notifications == null) {
                        daoNotifications.insert(newNotificationInf, false);
                    } else {
                        // update info in database
                        daoNotifications.update(newNotificationInf,
                                checkoutRecord.circId);
                    }

                    Intent intentNotification = new Intent(this,
                            NotificationReceiver.class);

                    Log.d(TAG, "Set due date alarm at" + cal.getTime()
                            + " for " + newNotificationInf.id + " intent_val: "
                            + newNotificationInf.intent_val);

                    intentNotification.putExtra("checkoutMessage",
                            "The item " + checkoutRecord.getAuthor()
                                    + " is about to expire on "
                                    + checkoutRecord.getDueDateString());
                    // update the current intent if it exists
                    PendingIntent sender = PendingIntent.getBroadcast(this,
                            NotificationAlert.NOTIFICATION_INTENT
                                    + checkoutRecord.circId,
                            intentNotification,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    // Get the AlarmManager service
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                            sender);
                }
            }
            daoNotifications.close();

            lastUpdateServiceDate = currentDate;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("lastUpdateTime", lastUpdateServiceDate.getTime());
            editor.commit();

            Log.d(TAG, "set last service update date " + lastUpdateServiceDate);
        }
        */
    }
}
