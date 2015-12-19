package org.evergreen_ils.globals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** private logging class that maintains a queue for potential sharing
 * Created by kenstir on 12/9/2015.
 */
public class Log {
    private static final int mQueueSize = 200;
    private static ArrayDeque<String> mEntries = new ArrayDeque<String>(mQueueSize);
    private static SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    // caller is synchronized
    public static synchronized void push(String TAG, String msg, Throwable tr) {
        android.util.Log.d(TAG, msg, tr);

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

    public static void d(String TAG, String msg) {
        push(TAG, msg, null);
    }

    public static void d(String TAG, String msg, Throwable tr) {
        push(TAG, msg, tr);
    }

    public static void i(String TAG, String msg) {
        push(TAG, msg, null);
    }

    public static void v(String TAG, String msg) {
        push(TAG, msg, null);
    }

    public static void w(String TAG, String msg) {
        push(TAG, msg, null);
    }

    public static void w(String TAG, Throwable tr) {
        push(TAG, null, tr);
    }

    public static void w(String TAG, String msg, Throwable tr) {
        push(TAG, msg, tr);
    }

    public static long logElapsedTime(String TAG, long start_ms, String s) {
        long now_ms = System.currentTimeMillis();
        Log.d(TAG, "elapsed: " + s + ": " + (now_ms - start_ms) + "ms");
        return now_ms;
    }
}
