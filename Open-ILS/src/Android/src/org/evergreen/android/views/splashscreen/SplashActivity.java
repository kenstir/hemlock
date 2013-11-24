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
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.splashscreen.LoadingTask.LoadingTaskListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity implements LoadingTaskListener {

    private TextView progressText;
    private Context context;
    private ProgressBar progressBar;
    private String TAG = "SplashActivity";
    private AlertDialog alertDialog;
    private SharedPreferences prefs;
    private LoadingTask task;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        this.context = this;

        progressText = (TextView) findViewById(R.id.action_in_progress);
        progressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);

        prefs= PreferenceManager.getDefaultSharedPreferences(context);
        GlobalConfigs.httpAddress = context.getString(R.string.ou_library_url);
        String username = prefs.getString("username", "");
        AccountAccess.setAccountName(username);
        
        task = new LoadingTask(this, this);
        task.execute(new String());
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
        Log.d(TAG, "onActivityResult> "+requestCode+" "+resultCode);
    }

    private void startApp() {
        Intent intent = new Intent(SplashActivity.this, SearchCatalogListView.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressUpdate(String value) {
        progressText.setText(value);
    }

    @Override
    public void onPostExecute(String result) {
        Log.d(TAG, "onPostExecute> "+result);
        progressBar.setVisibility(View.GONE);
        if (result.equals(LoadingTask.TASK_OK)) {
            startApp();
        } else {
            String extra_text = "...Failed";
            if (!TextUtils.isEmpty(result)) extra_text = extra_text + ": " + result;
            progressText.setText(progressText.getText() + extra_text);
            //retryButton.setVisibility(View.VISIBLE);
        }
    }
}