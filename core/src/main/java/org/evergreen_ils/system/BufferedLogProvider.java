package org.evergreen_ils.system;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by kenstir on 3/11/2017.
 */

public class BufferedLogProvider implements LogProvider {
    private static final int mQueueSize = 200;
    private static ArrayDeque<String> mEntries = new ArrayDeque<>(mQueueSize);
    private static SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static synchronized void println(int priority, String TAG, String msg, Throwable tr) {
        if (msg == null) return;

        // log to android too
        if (tr != null) {
            msg = msg + '\n' + android.util.Log.getStackTraceString(tr);
        }
        android.util.Log.println(priority, TAG, msg);

        // format as a string
        StringBuilder sb = new StringBuilder();
        String date = mTimeFormat.format(System.currentTimeMillis());
        sb.append(date).append('\t').append(TAG);

// we have already appended the stack trace to the msg
//        String prefix = sb.toString();
//
//        if (msg != null) {
//            mEntries.push(prefix + '\t' + msg);
//        }
//
//        if (tr != null) {
//            mEntries.push(prefix + '\t' + tr.getMessage());
//            StackTraceElement backtrace[] = tr.getStackTrace();
//            for (StackTraceElement elem : backtrace) {
//                mEntries.push(prefix + "\t at " + elem.toString());
//            }
//        }

        sb.append('\t').append(msg);
        mEntries.push(sb.toString());

        while (mEntries.size() > mQueueSize) {
            mEntries.pop();
        }
    }

    @Override
    public synchronized String getLogBuffer() {
        StringBuilder sb = new StringBuilder(mQueueSize * 120);
        Iterator<String> it = mEntries.descendingIterator();
        while (it.hasNext()) {
            sb.append(it.next()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public void d(String TAG, String msg) {
        println(android.util.Log.DEBUG, TAG, msg, null);
    }

    @Override
    public void d(String TAG, String msg, Throwable tr) {
        println(android.util.Log.DEBUG, TAG, msg, tr);
    }

    @Override
    public void i(String TAG, String msg) {
        println(android.util.Log.INFO, TAG, msg, null);
    }

    @Override
    public void w(String TAG, String msg) {
        println(android.util.Log.WARN, TAG, msg, null);
    }

    @Override
    public void w(String TAG, String msg, Throwable tr) {
        println(android.util.Log.WARN, TAG, msg, tr);
    }
}
