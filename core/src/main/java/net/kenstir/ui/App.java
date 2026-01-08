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

    // TODO: factor out LoaderService.makeHttpClient()
    public static void configureHttpClient(Context context) {
        GatewayClient.cacheDirectory = new File(context.getCacheDir(), "okhttp");
        GatewayClient.initHttpClient();
        CoilImageLoader.INSTANCE.setImageLoader(context, GatewayClient.okHttpClient);
    }

    static public void init(Context context) {
        if (mInitialized) {
            return;
        }
        boolean isAndroidTest = context.getResources().getBoolean(R.bool.is_android_test);
        Log.d(TAG, "[init] App.init isAndroidTest=" + isAndroidTest);
        configureHttpClient(context);
        mInitialized = true;
    }

    @Nullable
    public static String getFcmNotificationToken() {
        return fcmNotificationToken;
    }

    public static void setFcmNotificationToken(@Nullable String fcmNotificationToken) {
        App.fcmNotificationToken = fcmNotificationToken;
    }

    public static @NonNull Account getAccount() {
        return account;
    }

    public static void setAccount(@NonNull Account account) {
        App.account = account;
    }
}
