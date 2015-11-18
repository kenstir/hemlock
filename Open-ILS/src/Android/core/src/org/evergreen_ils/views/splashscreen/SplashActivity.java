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
package org.evergreen_ils.views.splashscreen;

import android.content.SharedPreferences;
import org.evergreen_ils.R;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.views.MainActivity;
import org.evergreen_ils.views.splashscreen.LoadingTask.LoadingTaskListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity implements LoadingTaskListener {

    public final static int REQUEST_SELECT_LIBRARY = 0;
    private static String TAG = SplashActivity.class.getSimpleName();
    private TextView mProgressText;
    private Context mContext;
    private ProgressBar mProgressBar;
    private AlertDialog mAlertDialog;
    private Button mRetryButton;
    private LoadingTask mTask;
    private static boolean mInitialized;
    private boolean restarted = false;

    public static boolean isAppInitialized() {
        return mInitialized;
    }

    /**
     * android may choose to initialize the app at a non-MAIN activity if the
     * app crashed or for other reasons.  In these cases we want to force sane
     * initialization via the SplashActivity.
     * <p/>
     * used in all activity class's onCreate() like so:
     * <code>
     * if (!SplashActivity.isInitialized) {
     * SplashActivity.restartApp(this);
     * return;
     * }
     * </code>
     *
     * @param a
     */
    public static void restartApp(Activity a) {
        Log.d(TAG, "restartApp> Restarting SplashActivity");
        Intent i = new Intent(a, SplashActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        a.startActivity(i);
        a.finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mContext = this;
        setContentView(R.layout.activity_splash);

        AppPrefs.init(getApplicationContext());

        mProgressText = (TextView) findViewById(R.id.action_in_progress);
        mProgressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);
        mRetryButton = (Button) findViewById(R.id.activity_splash_retry_button);
        mRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });
    }

    protected void startTask() {
        Log.d(TAG, "startTask> task=" + mTask);
        if (mTask != null)
            return;
        mTask = new LoadingTask(this, this);
        mTask.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onstart");
        if (!restarted) {
            startTask();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restarted = true;
        Log.d(TAG, "onrestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onresume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onstop");
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onactivityresult: " + requestCode + " " + resultCode);
    }

    private void startApp() {
        mInitialized = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPreExecute() {
        mRetryButton.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressUpdate(String value) {
        mProgressText.setText(value);
    }

    @Override
    public void onPostExecute(String result) {
        Log.d(TAG, "onPostExecute> " + result);
        mTask = null;
        mProgressBar.setVisibility(View.GONE);
        if (TextUtils.equals(result, LoadingTask.TASK_OK)) {
            startApp();
        } else {
            String extra_text;
            if (!TextUtils.isEmpty(result))
                extra_text = "...Failed:\n" + result;
            else
                extra_text = "...Cancelled";
            Log.d(TAG, "progresstext += " + extra_text);
            mProgressText.setText(mProgressText.getText() + extra_text);
            mRetryButton.setVisibility(View.VISIBLE);
        }
    }
}