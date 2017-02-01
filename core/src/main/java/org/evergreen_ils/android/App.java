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
import org.evergreen_ils.system.Log;

import java.io.File;

/**
 * Created by kenstir on 1/29/2017.
 */
public class App {
    private static final String TAG = App.class.getSimpleName();

    public static final int ITEM_PLACE_HOLD = 0;
    public static final int ITEM_SHOW_DETAILS = 1;
    public static final int ITEM_ADD_TO_LIST = 2;

    private static int mIsDebuggable = -1;

    public static boolean getIsDebuggable(Context context) {
        if (mIsDebuggable < 0)
            mIsDebuggable = (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
        return mIsDebuggable > 0;
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
}
