/*
 * Copyright (C) 2015 Kenneth H. Cox
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

package org.evergreen_ils.globals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.*;

/** private logging class that maintains a queue for potential sharing
 * Created by kenstir on 12/9/2015.
 */
public class Log {

    private static void println(int priority, String TAG, String msg, Throwable tr) {
        if (tr != null) {
            msg = msg + '\n' + android.util.Log.getStackTraceString(tr);
        }
        android.util.Log.println(priority, TAG, msg);
    }

    public static synchronized String getString(Context context) {
        return null;
    }

    /*
    private static final int mQueueSize = 200;
    private static ArrayDeque<String> mEntries = new ArrayDeque<String>(mQueueSize);
    private static SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static synchronized void println(int priority, String TAG, String msg, Throwable tr) {
        if (tr != null) {
            msg = msg + '\n' + android.util.Log.getStackTraceString(tr);
        }
        android.util.Log.println(priority, TAG, msg);
        StringBuilder sb = new StringBuilder();
        String date = mTimeFormat.format(System.currentTimeMillis());
        sb.append(date).append('\t').append(TAG);
        String prefix = sb.toString();

        if (msg != null) {
            mEntries.push(prefix + '\t' + msg);
        }

        if (tr != null) {
            mEntries.push(prefix + '\t' + tr.getMessage());
            StackTraceElement backtrace[] = tr.getStackTrace();
            for (StackTraceElement elem : backtrace) {
                mEntries.push(prefix + "\t at " + elem.toString());
            }
        }

        while (mEntries.size() > mQueueSize) {
            mEntries.pop();
        }
    }

    public static String getAppInfo(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Log", "caught", e);
        }
        String versionName = pInfo.versionName;
        int verCode = pInfo.versionCode;
        String version = "" + verCode + " (" + versionName + ")\n";
        return version;
    }

    public static synchronized String getString(Context context) {
        StringBuilder sb = new StringBuilder(mQueueSize * 120);
        sb.append(getAppInfo(context));
        Iterator<String> it = mEntries.descendingIterator();
        while (it.hasNext()) {
            sb.append(it.next()).append('\n');
        }
        return sb.toString();
    }
    */

    public static void d(String TAG, String msg) {
        println(android.util.Log.DEBUG, TAG, msg, null);
    }

    public static void d(String TAG, String msg, Throwable tr) {
        println(android.util.Log.DEBUG, TAG, msg, tr);
    }

    public static void i(String TAG, String msg) {
        println(android.util.Log.INFO, TAG, msg, null);
    }

    public static void v(String TAG, String msg) {
        println(android.util.Log.VERBOSE, TAG, msg, null);
    }

    public static void w(String TAG, String msg) {
        println(android.util.Log.WARN, TAG, msg, null);
    }

    public static void w(String TAG, Throwable tr) {
        println(android.util.Log.WARN, TAG, null, tr);
    }

    public static void w(String TAG, String msg, Throwable tr) {
        println(android.util.Log.WARN, TAG, msg, tr);
    }

    public static long logElapsedTime(String TAG, long start_ms, String s) {
        long now_ms = System.currentTimeMillis();
        Log.d(TAG, "elapsed: " + s + ": " + (now_ms - start_ms) + "ms");
        return now_ms;
    }
}
