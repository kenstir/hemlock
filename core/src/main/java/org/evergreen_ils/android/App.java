/*
 * Copyright (C) 2017 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.evergreen_ils.R;
import org.evergreen_ils.data.Account;
import org.evergreen_ils.data.Library;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.net.Volley;
import org.evergreen_ils.utils.ui.AppState;
import org.evergreen_ils.views.MenuProvider;
import org.evergreen_ils.views.launch.LaunchActivity;
import org.evergreen_ils.views.MainActivity;

import java.io.File;

// TODO: This functionality could be moved to our custom Application class
public class App {
    private static final String TAG = App.class.getSimpleName();

    // request/result codes for use with startActivityForResult
    public static final int REQUEST_MESSAGES = 10002;

    public static boolean mStarted = false;
    private static boolean mCachedEnabled = false;

    private static AppBehavior behavior = null;
    private static Library library = null;
    private static Account account = null;

    public static int getVersionCode(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Log", "caught", e);
            return 0;
        }
    }

    public static String getAppInfo(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Log", "caught", e);
        }
        String appName = context.getString(R.string.ou_app_label);
        String versionName = pInfo.versionName;
        int verCode = pInfo.versionCode;
        String version = appName + " " + verCode + " (" + versionName + ")";
        return version;
    }

    public static void enableCaching(Context context) {
        if (mCachedEnabled)
            return;
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(context.getCacheDir(), "volley");//try to reuse same cache dir as volley
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            Log.d(TAG, "HTTP response cache is unavailable.");
        }
        mCachedEnabled = true;
    }

    static public void init(Context context) {
        enableCaching(context);
        if (behavior == null)
            behavior = AppFactory.makeBehavior(context.getResources());
        Volley.init(context);
        Gateway.clientCacheKey = Integer.toString(getVersionCode(context));
    }

    public static AppBehavior getBehavior() {
        return behavior;
    }

    public static Library getLibrary() {
        return library;
    }

    public static void setLibrary(Library library) {
        App.library = library;
        Gateway.baseUrl = library.getUrl();
    }

    /**
     * android may choose to initialize the app at a non-MAIN activity if the
     * app crashed or for other reasons.  In these cases we want to force sane
     * initialization via the LaunchActivity.
     * <p/>
     * used in all activity class's onCreate() like so:
     * <code>
     * if (!App.isStarted()) {
     *     App.restartApp(this);
     *     return;
     * }
     * </code>
     */
    public static void restartApp(Activity activity) {
        Analytics.log(TAG, "restartApp> Restarting");
        Intent i = new Intent(activity.getApplicationContext(), LaunchActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(i);
        activity.finish();
    }

    public static void startApp(Activity activity) {
        setStarted(true);
        updateLaunchCount();
        Intent intent = getMainActivityIntent(activity);
        activity.startActivity(intent);
        activity.finish();
    }

    public static Intent getMainActivityIntent(Activity activity) {
        String clazzName = activity.getString(R.string.ou_main_activity);
        if (!TextUtils.isEmpty(clazzName)) {
            try {
                Class clazz = Class.forName(clazzName);
                return new Intent(activity.getApplicationContext(), clazz);
            } catch (Exception e) {
                Analytics.logException(e);
            }
        }
        return new Intent(activity.getApplicationContext(), MainActivity.class);
    }

//    static void Class<?> getMainActivityClass(Activity activity) {
//        //String className = activity.getString(R.string.ou_main_activity);
//        return MainActivity.class;
//    }

    static void updateLaunchCount() {
        int launch_count = AppState.getInt(AppState.LAUNCH_COUNT);
        AppState.setInt(AppState.LAUNCH_COUNT, launch_count + 1);
    }

    public static boolean isStarted() {
        return mStarted;
    }

    private static void setStarted(boolean flag) {
        mStarted = flag;
    }

    public static Account getAccount() {
        return account;
    }

    public static void setAccount(Account account) {
        App.account = account;
    }
}
