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

import static net.kenstir.ui.account.AuthenticatorActivity.ARG_ACCOUNT_NAME;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;

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

public class App {
    private static final String TAG = "App";

    // request/result codes for use with startActivityForResult
    public static final int REQUEST_MESSAGES = 10002;

    public static boolean mStarted = false;
    private static boolean mInitialized = false;

    private static Library library = null;
    private static @NonNull Account account = Account.Companion.getNoAccount();
    private static String fcmNotificationToken = null;

    private static ServiceConfig mServiceConfig = null;

    // TODO: factor out LoaderService.makeHttpClient()
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
        boolean isAndroidTest = context.getResources().getBoolean(R.bool.is_android_test);
        Log.d(TAG, "[init] App.init isAndroidTest=" + isAndroidTest);
        configureHttpClient(context);
        if (mServiceConfig == null) {
            mServiceConfig = new ServiceConfig();
        }
        mInitialized = true;
    }

    public static @NonNull Library getLibrary() {
        return library;
    }

    public static void setLibrary(@NonNull Library library) {
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
        restartAppWithAccount(activity, null);
    }

    public static void restartAppWithAccount(Activity activity, @Nullable String accountName) {
        Analytics.log(TAG, "[init] restartApp " + (accountName != null ? "with " + accountName : ""));
        Intent intent = new Intent(activity.getApplicationContext(), LaunchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (accountName != null)
            intent.putExtra(ARG_ACCOUNT_NAME, accountName);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void startApp(Activity activity) {
        Analytics.log(TAG, "[init] startApp");
        setStarted(true);
        AppState.incrementLaunchCount();
        Intent intent = getMainActivityIntent(activity);
        activity.startActivity(intent);
        activity.finish();
    }

    /** Start app from a push notification */
    public static void startAppFromPushNotification(Activity activity, Class<? extends BaseActivity> targetActivityClass) {
        Analytics.log(TAG, "[init][fcm] startAppFromPushNotification");
        setStarted(true);
        AppState.incrementLaunchCount();

        // Start the app with a back stack, so if the user presses Back, the app does not exit.
        Intent mainIntent = getMainActivityIntent(activity)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent intent = new Intent(activity.getApplicationContext(), targetActivityClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = TaskStackBuilder.create(activity.getApplicationContext())
                .addNextIntent(mainIntent)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (pendingIntent != null) {
                pendingIntent.send();
            } else {
                activity.startActivity(intent);
                activity.finish();
            }
        } catch (PendingIntent.CanceledException e) {
            Analytics.logException(e);
        }
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

    public static boolean isStarted() {
        return mStarted;
    }

    private static void setStarted(boolean flag) {
        mStarted = flag;
    }

    public static @NonNull Account getAccount() {
        return account;
    }

    public static void setAccount(@NonNull Account account) {
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
