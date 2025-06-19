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

package net.kenstir.hemlock.android;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

/** private logging class that allows substituting different behaviors
 */
public class Log {
    // defining these statics here allows me to unit test code that logs / calls analytics
    // values from https://developer.android.com/reference/android/util/Log

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int WARN = 5;

    public static LogProvider provider = new AndroidLogProvider();
    public static int level = DEBUG;

    public static String TAG_ASYNC = "async";
    public static String TAG_FCM = "fcm";
    public static String TAG_PERM = "perm";

    public static void setProvider(LogProvider _provider) {
        provider = _provider;
    }

    public static void v(@NonNull String tag, @NonNull String msg) {
        if (provider != null && level <= VERBOSE) provider.v(tag, msg);
    }

    public static void d(@NonNull String TAG, @NonNull String msg) {
        if (provider != null && level <= DEBUG) provider.d(TAG, msg);
    }
    public static void d(@NonNull String TAG, @NonNull String msg, Throwable tr) {
        if (provider != null && level <= DEBUG) provider.d(TAG, msg, tr);
    }
    public static void i(@NonNull String TAG, @NonNull String msg) {
        if (provider != null) provider.i(TAG, msg);
    }
    public static void w(@NonNull String TAG, @NonNull String msg) {
        if (provider != null) provider.w(TAG, msg);
    }
    public static void w(@NonNull String TAG, @NonNull String msg, Throwable tr) {
        if (provider != null) provider.w(TAG, msg, tr);
    }

    @SuppressLint("DefaultLocale")
    public static long logElapsedTime(@NonNull String TAG, long start_ms, @NonNull String s) {
        long now_ms = System.currentTimeMillis();
        if (provider != null) provider.d(TAG, String.format("%3dms: %s", now_ms - start_ms, s));
        return now_ms;
    }
}
