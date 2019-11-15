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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.evergreen_ils.R;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.system.Library;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.AppState;

import java.io.File;

/**
 * Created by kenstir on 1/29/2017.
 */
public class App {
    private static final String TAG = App.class.getSimpleName();

    public static final int ITEM_PLACE_HOLD = 0;
    public static final int ITEM_SHOW_DETAILS = 1;
    public static final int ITEM_ADD_TO_LIST = 2;

    // request/result codes for use with startActivityForResult
    public static final int REQUEST_PURCHASE = 10001;
    public static final int REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT = 10002;
    public static final int RESULT_PURCHASED = 20001;

    private static int mIsDebuggable = -1;

    private static AppBehavior behavior = null;
    private static Library library = null;

    public static boolean getIsDebuggable(Context context) {
        if (mIsDebuggable < 0)
            mIsDebuggable = (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
        return mIsDebuggable > 0;
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
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(context.getCacheDir(), "volley");//try to reuse same cache dir as volley
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            Log.d(TAG, "HTTP response cache is unavailable.");
        }
    }

    static public void init(Context context) {
        AppState.init(context);
        if (behavior == null)
            behavior = AppFactory.makeBehavior(context.getResources());
        VolleyWrangler.init(context);
    }

    public static AppBehavior getBehavior() {
        return behavior;
    }

    public static Library getLibrary() { return library; }

    public static void setLibrary(Library library) {
        App.library = library;
    }
}
