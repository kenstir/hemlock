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

package org.evergreen_ils.system;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.LoginEvent;

import org.opensrf.Method;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/** Utils that wrap Crashlytics
 *
 * Created by kenstir on 12/5/2017.
 */

public class Analytics {
    private static final String TAG = Analytics.class.getSimpleName();
    private static final int MAX_PARAMS = 5;
    private static String mLastAuthToken = null;

    public static void initialize(Context context) {
        Fabric.with(context, new Crashlytics());
    }

    public static void setString(String key, String val) {
        Crashlytics.setString(key, val);
    }

    public static String redactedString(String val) {
        if (val == null) return "(null)";
        if (val.length() == 0) return "(empty)";
        return "***";
    }

    public static void log(String tag, String msg) {
        Crashlytics.log(Log.DEBUG, TAG, msg);
    }
    public static void log(String msg) {
        log(TAG, msg);
    }

    public static void logRequest(String service, Method method, String authToken) {
        mLastAuthToken = authToken;
        logRequest(service, method);
    }

    public static void logRequest(String service, String method, List<Object> params) {
        Crashlytics.setString("svc", service);
        Crashlytics.setString("m", method);
        int i;
        List<String> logParams = new ArrayList<String>();
        for (i = 0; i < params.size(); i++) {
            String key = "p" + i;
            String val = "" + params.get(i);
            if (val.length() > 0 && TextUtils.equals(val, mLastAuthToken)) val = "***";//redacted
            logParams.add(val);
            if (i < MAX_PARAMS)
                Crashlytics.setString(key, val);
        }
        for (; i < MAX_PARAMS; i++) {
            String key = "p" + i;
            Crashlytics.setString(key, null);
        }
        Crashlytics.log(Log.DEBUG, TAG, method
                + "(" + TextUtils.join(", ", logParams) + ")");
    }

    public static void logRequest(String service, Method method) {
        logRequest(service, method.getName(), method.getParams());
    }

    public static String buildGatewayUrl(String service, String method, Object[] params) {
        Analytics.logVolleyRequest(service, method, params);
        return Utils.buildGatewayUrl(service, method, params);
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
        try {
            if (resp instanceof OSRFObject) {
                OSRFObject o = (OSRFObject) resp;
                String netClass = o.getRegistry().getNetClass();
                String s = redactResponse(o, netClass);
                Log.d(TAG, "resp [" + netClass + "]: " + s);
                Crashlytics.log(Log.DEBUG, TAG, "resp [" + netClass + "]: " + s);
                return;
            }
        } catch (Exception e) {
            Crashlytics.log(Log.DEBUG, TAG, "exception parsing resp: " + e.getMessage());
        }
        Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp);
    }

    public static void logResponse(GatewayResponse resp) {
        Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp.payload);
    }

    public static void logRedactedResponse() {
        Crashlytics.log(Log.DEBUG, TAG, "resp: ***");
    }

    public static void logVolleyResponse(String method) {
        Crashlytics.log(Log.DEBUG, TAG, method + " ok");
    }

    public static void logErrorResponse(String resp) {
        Crashlytics.log(Log.WARN, TAG, "err_resp: " + resp);
    }

    public static void logException(String tag, Throwable e) {
        Log.d(tag, "caught", e);
        Crashlytics.logException(e);
    }
    public static void logException(Throwable e) {
        logException(TAG, e);
    }

    private static String bool2str(boolean val) { return val ? "true" : "false"; }

    private static void logEvent(CustomEvent ev) {
        Log.d(TAG, "logEvent "+ev.toString());
        Answers.getInstance().logCustom(ev);
    }
    public static void logEvent(String event) {
        CustomEvent ev = new CustomEvent(event);
        logEvent(ev);
    }
    public static void logEvent(String event, String name, String val) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val);
        logEvent(ev);
    }
    public static void logEvent(String event, String name, boolean val) {
        logEvent(event, name, bool2str(val));
    }
    public static void logEvent(String event, String name, Integer val) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val);
        logEvent(ev);
    }
    public static void logEvent(String event, String name, String val, String n2, String v2) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val)
                .putCustomAttribute(n2, v2);
        logEvent(ev);
    }
    public static void logEvent(String event, String name, Integer val, String n2, boolean v2) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val)
                .putCustomAttribute(n2, bool2str(v2));
        logEvent(ev);
    }
    public static void logEvent(String event, String name, boolean val, String n2, boolean v2) {
        logEvent(event, name, bool2str(val), n2, bool2str(v2));
    }
    public static void logEvent(String event, String name, String val, String n2, String v2, String n3, String v3) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, v3);
        logEvent(ev);
    }
    public static void logEvent(String event, String name, String val, String n2, String v2, String n3, long v3) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(name, val)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, v3);
        logEvent(ev);
    }
    public static void logEvent(String event, String n, String v, String n2, String v2, String n3, String v3, String n4, String v4) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(n, v)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, v3)
                .putCustomAttribute(n4,v4);
        logEvent(ev);
    }
    public static void logEvent(String event, String n, String v, String n2, String v2, String n3, boolean v3, String n4, String v4) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(n, v)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, bool2str(v3))
                .putCustomAttribute(n4, v4);
        logEvent(ev);
    }
    public static void logEvent(String event, String n, String v, String n2, String v2, String n3, boolean v3, String n4, boolean v4) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(n, v)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, bool2str(v3))
                .putCustomAttribute(n4, bool2str(v4));
        logEvent(ev);
    }
    public static void logEvent(String event, String n, Integer v, String n2, String v2, String n3, String v3, String n4, String v4) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(n, v)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, v3)
                .putCustomAttribute(n4, v4);
        logEvent(ev);
    }
    public static void logEvent(String event, String n, String v, String n2, String v2, String n3, long v3, String n4, boolean v4, String n5, long v5) {
        CustomEvent ev = new CustomEvent(event);
        ev.putCustomAttribute(n, v)
                .putCustomAttribute(n2, v2)
                .putCustomAttribute(n3, v3)
                .putCustomAttribute(n4, bool2str(v4))
                .putCustomAttribute(n5, v5);
        logEvent(ev);
    }
}
