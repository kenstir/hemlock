/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kenstir.hemlock.R;
import net.kenstir.ui.util.CoilImageLoader;
import net.kenstir.logging.Log;
import net.kenstir.data.service.ServiceConfig;
import org.evergreen_ils.gateway.GatewayClient;
import net.kenstir.data.model.Account;
import net.kenstir.data.model.Library;

import net.kenstir.ui.view.launch.LaunchActivity;
import net.kenstir.ui.view.main.MainActivity;
import net.kenstir.util.Analytics;

import java.io.File;

// TODO: This functionality could be moved to our custom Application class
public class App {
    private static final String TAG = "App";

    // request/result codes for use with startActivityForResult
    public static final int REQUEST_MESSAGES = 10002;

    public static boolean mStarted = false;
    private static boolean mInitialized = false;

    private static AppBehavior behavior = null;
    private static Library library = null;
    private static Account account = null;
    private static String fcmNotificationToken = null;

    private static ServiceConfig mServiceConfig = null;

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

    public static String getVersion(Context context) {
        return Integer.toString(getVersionCode(context));
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

    public static void configureHttpClient(Context context) {
        GatewayClient.cacheDirectory = new File(context.getCacheDir(), "okhttp");
        GatewayClient.initHttpClient();
        CoilImageLoader.INSTANCE.setImageLoader(context, GatewayClient.okHttpClient);
    }

    static public void init(Context context) {
        if (mInitialized) {
            //Log.d(TAG, "[init] App.init already done");
            return;
        }
        Log.d(TAG, "[init] App.init");
        configureHttpClient(context);
        behavior = AppFactory.makeBehavior(context.getResources());
        if (mServiceConfig == null) {
            mServiceConfig = new ServiceConfig();
        }
        mInitialized = true;
    }

    public static AppBehavior getBehavior() {
        return behavior;
    }

    public static Library getLibrary() {
        return library;
    }

    public static void setLibrary(Library library) {
        App.library = library;
        // TODO: set baseUrl via Service method in the data layer
        GatewayClient.baseUrl = library.getUrl();
    }

    @Nullable
    public static String getFcmNotificationToken() {
        return fcmNotificationToken;
    }

    public static void setFcmNotificationToken(@Nullable String fcmNotificationToken) {
        App.fcmNotificationToken = fcmNotificationToken;
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
        Analytics.log(TAG, "[init] restartApp");
        Intent i = new Intent(activity.getApplicationContext(), LaunchActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(i);
        activity.finish();
    }

    public static void startApp(Activity activity) {
        Analytics.log(TAG, "[init] startApp");
        setStarted(true);
        updateLaunchCount();
        Intent intent = getMainActivityIntent(activity);
        activity.startActivity(intent);
        activity.finish();
    }

    /** Start app from a push notification */
    public static void startAppFromPushNotification(Activity activity, Class<? extends BaseActivity> targetActivityClass) {
        Analytics.log(TAG, "[init] startAppFromPushNotification");
        setStarted(true);
        updateLaunchCount();
        Intent intent = new Intent(activity.getApplicationContext(), targetActivityClass);
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

    @NonNull
    public static ServiceConfig getServiceConfig() {
        return mServiceConfig;
    }

    public static void setServiceConfig(@NonNull ServiceConfig serviceConfig) {
        App.mServiceConfig = serviceConfig;
    }
}
