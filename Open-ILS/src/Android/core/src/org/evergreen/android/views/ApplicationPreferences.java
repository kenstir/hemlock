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
package org.evergreen.android.views;

import java.util.Calendar;

import org.evergreen.android.R;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.services.NotificationAlert;
import org.evergreen.android.services.PeriodicServiceBroadcastReceiver;
import org.evergreen.android.services.ScheduledIntentService;
import org.evergreen_ils.auth.Const;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

public class ApplicationPreferences extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private String TAG = ApplicationPreferences.class.getSimpleName();

    private ProgressDialog progressDialog;

    private ApplicationPreferences reference;

    private Context context;

    private Thread connectionThread = null;

    private Thread coreFilesDownload = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        context = this;
        reference = this;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // register preference listener
        prefs.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        reference = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        reference = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        reference = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        boolean httpAddressChange = false;

        connectionThread = new Thread(new Runnable() {

            @Override
            public void run() {
            }
        });

        boolean checkConnection = false;

        if (key.equals(Const.KEY_LIBRARY_URL)) {
            checkConnection = true;
            GlobalConfigs.httpAddress = sharedPreferences.getString(Const.KEY_LIBRARY_URL, "");

            httpAddressChange = true;
            System.out.println("Show dialog");

            progressDialog = ProgressDialog.show(context, "Core files",
                    "Downloading FM_IDL and OrgTree");

            coreFilesDownload = new Thread(new Runnable() {

                @Override
                public void run() {
                    System.out.println("FM idl download");
                    GlobalConfigs sg = GlobalConfigs.getGlobalConfigs(context);
                    sg.loadIDLFile(context);
                    sg.getOrganisations();
                    sg.getCopyStatusesAvailable((ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });

                    connectionThread.start();
                }
            });

            coreFilesDownload.start();

            // wait for execution

        }
        if (key.equals("notifications_enabled")) {

            if (sharedPreferences.getBoolean("notifications_enabled", false)) {

                Toast.makeText(context, "Set up notification updates",
                        Toast.LENGTH_SHORT).show();
                // if enabled register the update service to run once per day
                // get a Calendar object with current time
                Calendar cal = Calendar.getInstance();

                Intent bRecvIntent = new Intent(this,
                        PeriodicServiceBroadcastReceiver.class);
                bRecvIntent.setAction(ScheduledIntentService.ACTION);
                // update the current intent if it exists
                PendingIntent sender = PendingIntent.getBroadcast(this,
                        NotificationAlert.NOTIFICATION_INTENT
                                + PeriodicServiceBroadcastReceiver.INTENT_ID,
                        bRecvIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Get the AlarmManager service
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(),
                        10000 * ScheduledIntentService.SCHEDULE_TIME_INTERVAL,
                        sender);
            } else {
                Toast.makeText(context, "Disable notification updates",
                        Toast.LENGTH_SHORT).show();
                // cancel the service

                Intent bRecvIntent = new Intent(this,
                        PeriodicServiceBroadcastReceiver.class);

                // update the current intent if it exists
                PendingIntent sender = PendingIntent.getBroadcast(this,
                        NotificationAlert.NOTIFICATION_INTENT
                                + PeriodicServiceBroadcastReceiver.INTENT_ID,
                        bRecvIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Get the AlarmManager service
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                // cancel the service
                am.cancel(sender);
            }
        }
    }

    /*
     * Dialog interface for starting the network settings
     */
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // Yes button clicked

                context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                break;

            case DialogInterface.BUTTON_NEGATIVE:
                // No button clicked

                break;
            }
        }
    };

}
