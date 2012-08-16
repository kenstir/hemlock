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
package org.evergreen.android.views.splashscreen;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.ConfigureApplicationActivity;
import org.evergreen.android.views.splashscreen.LoadingTask.LoadingTaskFinishedListener;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity implements
        LoadingTaskFinishedListener {

    private TextView progressText;

    private Context context;

    private ProgressBar progressBar;

    private String TAG = "SplashActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        // Show the splash screen
        setContentView(R.layout.activity_splash);

        progressText = (TextView) findViewById(R.id.action_in_progress);

        // Find the progress bar
        progressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);

        boolean abort = false;

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        GlobalConfigs.httpAddress = prefs.getString("library_url", "");
        String username = prefs.getString("username", "username");
        String password = prefs.getString("password", "pas");
        AccountAccess.setAccountInfo(username, password);
        try {

            Utils.checkNetworkStatus((ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE));
        } catch (NoNetworkAccessException e) {
            abort = true;
            e.printStackTrace();
        } catch (NoAccessToServer e) {
            abort = true;

            // dialog.show();
            Intent configureIntent = new Intent(this,
                    ConfigureApplicationActivity.class);
            startActivityForResult(configureIntent, 0);

        }

        if (abort != true) {
            // Start your loading
            new LoadingTask(progressBar, this, this, progressText, this)
                    .execute("download"); // Pass in whatever you need a url is
                                          // just an example we don't use it
                                          // in this tutorial
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
        case ConfigureApplicationActivity.RESULT_CONFIGURE_SUCCESS: {
            new LoadingTask(progressBar, this, this, progressText, this)
                    .execute("download");

        }
            break;

        }
    }

    // This is the callback for when your async task has finished
    @Override
    public void onTaskFinished() {
        completeSplash();
    }

    private void completeSplash() {
        startApp();
        finish(); // Don't forget to finish this Splash Activity so the user
                  // can't return to it!
    }

    private void startApp() {
        Intent intent = new Intent(SplashActivity.this,
                SearchCatalogListView.class);
        startActivity(intent);
    }
}