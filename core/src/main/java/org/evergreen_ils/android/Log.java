/*
 * Copyright (c) 2020 Kenneth H. Cox
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

/** private logging class that allows substituting different behaviors,
 * Created by kenstir on 12/9/2015.
 */
public class Log {
    public static LogProvider provider = new AndroidLogProvider();

    // defining these statics here allows me to unit test code that logs / calls analytics
    // values from https://developer.android.com/reference/android/util/Log
    public static final int DEBUG = 3;
    public static final int WARN = 5;

    public static void setProvider(LogProvider _provider) {
        provider = _provider;
    }
    public static String getLogBuffer() {
        if (provider != null) return provider.getLogBuffer();
        return null;
    }

    public static void d(String TAG, String msg) {
        if (provider != null) provider.d(TAG, msg);
    }
    public static void d(String TAG, String msg, Throwable tr) {
        if (provider != null) provider.d(TAG, msg, tr);
    }
    public static void i(String TAG, String msg) {
        if (provider != null) provider.i(TAG, msg);
    }
    public static void w(String TAG, String msg) {
        if (provider != null) provider.w(TAG, msg);
    }
    public static void w(String TAG, String msg, Throwable tr) {
        if (provider != null) provider.w(TAG, msg, tr);
    }

    public static long logElapsedTime(String TAG, long start_ms, String s) {
        long now_ms = System.currentTimeMillis();
        if (provider != null) provider.d(TAG, String.format("%3dms elapsed: %s", now_ms - start_ms, s));
        return now_ms;
    }
}
