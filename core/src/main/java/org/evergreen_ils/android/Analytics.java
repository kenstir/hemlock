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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.evergreen_ils.net.Gateway;
import org.opensrf.Method;
import org.opensrf.util.GatewayResult;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Utils that wrap Crashlytics (and now Analytics)
 */
public class Analytics {
    private static final String TAG = Analytics.class.getSimpleName();
    private static final int MAX_PARAMS = 5;
    private static String mLastAuthToken = null;
    private static boolean analytics = false;
    private static FirebaseAnalytics mAnalytics = null;

    public static void initialize(Context context) {
        if (mAnalytics == null) mAnalytics = FirebaseAnalytics.getInstance(context);
        analytics = true;
//        if (!App.getIsDebuggable(context)) {
//            // only enable bug tracking in release version
//            Fabric.with(context, new Crashlytics());
//            analytics = true;
//        }
    }

    public static void setString(String key, String val) {
//        if (analytics) Crashlytics.setString(key, val);
    }

    public static String redactedString(String val) {
        if (val == null) return "(null)";
        if (val.length() == 0) return "(empty)";
        return "***";
    }

    public static void log(String tag, String msg) {
        Log.d(tag, msg);
        if (analytics) FirebaseCrashlytics.getInstance().log(msg);
    }
    public static void log(String msg) {
        log(TAG, msg);
    }

    public static void logRequest(String service, Method method, String authToken) {
        mLastAuthToken = authToken;
        logRequest(service, method);
    }

    public static void logRequest(String service, String method, List<Object> params) {
        if (!analytics) return;

        FirebaseCrashlytics.getInstance().setCustomKey("svc", service);
        FirebaseCrashlytics.getInstance().setCustomKey("m", method);
        int i;
        List<String> logParams = new ArrayList<String>();
        for (i = 0; i < params.size(); i++) {
            String key = "p" + i;
            String val = "" + params.get(i);
            if (val.length() > 0 && TextUtils.equals(val, mLastAuthToken)) val = "***";//redacted
            logParams.add(val);
            if (i < MAX_PARAMS)
                FirebaseCrashlytics.getInstance().setCustomKey(key, val);
        }
        for (; i < MAX_PARAMS; i++) {
            String key = "p" + i;
            FirebaseCrashlytics.getInstance().setCustomKey(key, null);
        }
//        Crashlytics.log(Log.DEBUG, TAG, method
//                + "(" + TextUtils.join(", ", logParams) + ")");
    }

    public static void logRequest(String service, Method method) {
        logRequest(service, method.getName(), method.getParams());
    }

    public static String buildGatewayUrl(String service, String method, Object[] params) {
        Analytics.logVolleyRequest(service, method, params);
        return Gateway.INSTANCE.buildUrl(service, method, params);
    }

    public static void logVolleyRequest(String service, String method, Object[] params) {
        List<Object> p = Arrays.asList(params);
        logRequest(service, method, p);
    }

    private static String redactResponse(OSRFObject o, String netClass) {
        if (netClass.equals("au") /*user*/ || netClass.equals("aou") /*orgTree*/) {
            return "***";
        } else {
            return o.toString();
        }
    }

    public static void logResponse(Object resp) {
        if (!analytics) return;

        try {
            if (resp instanceof OSRFObject) {
                OSRFObject o = (OSRFObject) resp;
                String netClass = o.getRegistry().getNetClass();
                String s = redactResponse(o, netClass);
//                Crashlytics.log(Log.DEBUG, TAG, "resp [" + netClass + "]: " + s);
                return;
            }
        } catch (Exception e) {
//            Crashlytics.log(Log.DEBUG, TAG, "exception parsing resp: " + e.getMessage());
        }
//        Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp);
    }

    public static void logResponse(GatewayResult resp) {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp.payload);
    }

    public static void logRedactedResponse() {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, "resp: ***");
    }

    public static void logVolleyResponse(String method) {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, method + " ok");
    }

    public static void logErrorResponse(String resp) {
//        if (analytics) Crashlytics.log(Log.WARN, TAG, "err_resp: " + resp);
    }

    public static void logException(String tag, Throwable e) {
        Log.d(tag, "caught", e);
        if (analytics) FirebaseCrashlytics.getInstance().recordException(e);
    }
    public static void logException(Throwable e) {
        logException(TAG, e);
    }

    private static String bool2str(boolean val) { return val ? "true" : "false"; }

    public static void logEvent(String event, Bundle b) {
        if (analytics) mAnalytics.logEvent(event, b);
    }
    public static void logEvent(String event) {
        logEvent(event, null);
    }
    public static void logEvent(String event, String name, String val) {
        Bundle b = new Bundle();
        b.putString(name, val);
        logEvent(event, b);
    }
    public static void logEvent(String event, String name, boolean val) {
        Bundle b = new Bundle();
        b.putBoolean(name, val);
        logEvent(event, b);
    }
    public static void logEvent(String event, String name, Integer val) {
        Bundle b = new Bundle();
        b.putInt(name, val);
        logEvent(event, b);
    }
    public static void logEvent(String event, String name, String val, String n2, String v2) {
        Bundle b = new Bundle();
        b.putString(name, val);
        b.putString(n2, v2);
        logEvent(event, b);
    }
    public static void logEvent(String event, String name, Integer val, String n2, boolean v2) {
        Bundle b = new Bundle();
        b.putInt(name, val);
        b.putBoolean(n2, v2);
        logEvent(event, b);
    }
}
