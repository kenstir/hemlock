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
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
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
    
    private AlertDialog alertDialog;
    
    
    private static final int ABORT = 1;
    private static final int ABORT_SERVER_PROBLEM = 2;
    private static final int ABORT_NETWORK_CONN_PROLEM = 3;
    private static final int OK = 0;
    private int abort;
    private SharedPreferences prefs;
    private SplashActivity activity;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        activity = this;
        // Show the splash screen
        setContentView(R.layout.activity_splash);

        progressText = (TextView) findViewById(R.id.action_in_progress);

        // Find the progress bar
        progressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);

        abort = OK;
        prefs= PreferenceManager
                .getDefaultSharedPreferences(context);
        GlobalConfigs.httpAddress = prefs.getString("library_url", "");
        String username = prefs.getString("username", "username");
        String password = prefs.getString("password", "pas");
        AccountAccess.setAccountInfo(username, password);

        
        
        Thread checkConnThread = new Thread(new Runnable() {
            
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Utils.checkNetworkStatus((ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE));
                } catch (NoNetworkAccessException e) {
                    abort = ABORT_NETWORK_CONN_PROLEM;
                    e.printStackTrace();
                } catch (NoAccessToServer e) {
                    // you have no access to server or server down
                    abort = ABORT_SERVER_PROBLEM;
                    e.printStackTrace();
                }
                
                runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        if(!prefs.contains("library_url")) {
                            Intent configureIntent = new Intent(context,
                                    ConfigureApplicationActivity.class);
                            startActivityForResult(configureIntent, 0);
                        }
                        
                        if (abort == OK) {
                            // Start your loading
                            new LoadingTask(progressBar, activity, activity, progressText, activity)
                                    .execute("download"); // Pass in whatever you need a url is
                                                          // just an example we don't use it
                                                          // in this tutorial
                        }
                        else
                        {

                            switch(abort) {
                            
                            case ABORT_NETWORK_CONN_PROLEM : {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                        context);
                         
                                    // set title
                                    alertDialogBuilder.setTitle("Network problem");
                         
                                    // set dialog message
                                    alertDialogBuilder
                                        .setMessage("You do not have your network activated. Please activate it!")
                                        .setCancelable(false)
                                        .setNegativeButton("exit",new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                // if this button is clicked, just close
                                                // the dialog box and do nothing
                                                dialog.cancel();
                                                finish();
                                            }
                                        });
                         
                                        // create alert dialog
                                        alertDialog = alertDialogBuilder.create();
                         
                                        // show it
                                        alertDialog.show();
                                
                            } break;
                            
                            case ABORT_SERVER_PROBLEM : {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                        context);
                         
                                    // set title
                                    alertDialogBuilder.setTitle("Evergreen server problem");
                         
                                    // set dialog message
                                    alertDialogBuilder
                                        .setMessage("Seams the server can't be found. Please configure the server address")
                                        .setCancelable(false)
                                        .setPositiveButton("Configure",new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                // if this button is clicked, close
                                                // current activity
                                                
                                                Intent configureIntent = new Intent(context, 
                                                        ConfigureApplicationActivity.class);
                                                startActivityForResult(configureIntent, 0);
                                                
                                            }
                                          })
                                        .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                // if this button is clicked, just close
                                                // the dialog box and do nothing
                                                dialog.cancel();
                                                finish();
                                            }
                                        });
                         
                                        // create alert dialog
                                        alertDialog = alertDialogBuilder.create();
                         
                                        // show it
                                        alertDialog.show();
                            } break;
                            
                            }

                        }
                    }
                });
            }
        });
        
        checkConnThread.start();
        
        
        
    }

    
    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if(alertDialog != null) {
            alertDialog.dismiss();
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