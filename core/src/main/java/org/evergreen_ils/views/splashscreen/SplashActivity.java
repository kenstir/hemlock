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

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.android.App;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.utils.ui.AppState;
import org.evergreen_ils.views.MainActivity;
import org.evergreen_ils.views.splashscreen.LoadingTask.LoadingTaskListener;
import org.opensrf.ShouldNotHappenException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity implements LoadingTaskListener {

    public final static int REQUEST_SELECT_LIBRARY = 0;
    private static String TAG = SplashActivity.class.getSimpleName();
    private TextView mProgressText;
    private View mProgressBar;
    private Button mRetryButton;
    private LoadingTask mTask;
    private long mStartTimeMillis;
    private static boolean mInitialized;

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
        Analytics.log(TAG, "restartApp> Restarting SplashActivity");
        Intent i = new Intent(a, SplashActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        a.startActivity(i);
        a.finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.initialize(this);
        App.init(this);
        applyNightMode();

        setContentView(R.layout.activity_splash);

        mProgressText = findViewById(R.id.action_in_progress);
        mProgressBar = findViewById(R.id.activity_splash_progress_bar);
        mRetryButton = findViewById(R.id.activity_splash_retry_button);
        mRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTask();
            }
        });

        startTask();
    }

    // TODO: factor out into common ThemeManager class
    protected void applyNightMode() {
        int nightMode = AppState.getInt(AppState.NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    protected synchronized void startTask() {
        Analytics.log(TAG, "startTask> task=" + mTask);
        if (mTask != null)
            return;
        mStartTimeMillis = System.currentTimeMillis();
        mTask = new LoadingTask(this, this);
        mTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Analytics.log(TAG, "onactivityresult: " + requestCode + " " + resultCode);
    }

    private void startApp() {
        mInitialized = true;
        updateLaunchCount();
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    void updateLaunchCount() {
        int launch_count = AppState.getInt(AppState.LAUNCH_COUNT);
        AppState.setInt(AppState.LAUNCH_COUNT, launch_count + 1);
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
        Analytics.log(TAG, "post> " + result);
        Account[] accounts = AccountUtils.getAccountsByType(this);
        Analytics.logEvent("Account: Login", "result", result,
                "library_name", AppState.getString(AppState.LIBRARY_NAME),
                "elapsed_ms", System.currentTimeMillis() - mStartTimeMillis,
                "multiple_accounts", accounts.length > 1,
                "num_accounts", accounts.length);
        mTask = null;
        if (TextUtils.equals(result, LoadingTask.TASK_OK)) {
            startApp();
        } else {
            String extra_text;
            if (!TextUtils.isEmpty(result)) {
                extra_text = " failed:\n" + result;
                if (result.contains("Malformed")) {
                    // 2018-11-19: debugging "MalformedURLException"
                    Analytics.log(TAG, "library_url=" + AppState.LIBRARY_URL);
                    Analytics.logException(new ShouldNotHappenException(result));
                }
            } else {
                extra_text = " cancelled";
            }
            mProgressText.setText(mProgressText.getText() + extra_text);
            mRetryButton.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }
}