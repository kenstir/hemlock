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

package org.evergreen_ils.utils.ui;

import android.content.Context;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.opensrf.Method;
import org.opensrf.util.GatewayResponse;

import java.util.Arrays;
import java.util.List;

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

    public static void log(String msg) {
        Crashlytics.log(android.util.Log.DEBUG, TAG, msg);
    }

    public static void logRequest(String service, Method method, String authToken) {
        mLastAuthToken = authToken;
        logRequest(service, method);
    }

    public static void logRequest(String service, String method, List<Object> params) {
        Crashlytics.log(android.util.Log.DEBUG, TAG, "req: " + method);
        Crashlytics.setString("svc", service);
        Crashlytics.setString("m", method);
        int i;
        for (i = 0; i < params.size(); i++) {
            String key = "p" + i;
            String val = "" + params.get(i);
            if (val.length() > 0 && TextUtils.equals(val, mLastAuthToken)) val = "***";//private
            Crashlytics.log(android.util.Log.DEBUG, TAG, " " + key + ": " + val);
            if (i < MAX_PARAMS)
                Crashlytics.setString(key, val);
        }
        for (; i < MAX_PARAMS; i++) {
            String key = "p" + i;
            Crashlytics.setString(key, null);
        }
    }

    public static void logRequest(String service, Method method) {
        logRequest(service, method.getName(), method.getParams());
    }

    public static void logVolleyRequest(String service, String method, Object[] params) {
        List<Object> p = Arrays.asList(params);
        logRequest(service, method, p);
    }

    public static void logResponse(Object resp) {
        Crashlytics.log(android.util.Log.INFO, TAG, "resp: " + resp);
    }

    public static void logResponse(GatewayResponse resp) {
        Crashlytics.log(android.util.Log.INFO, TAG, "resp: " + resp.map);
    }

    public static void logErrorResponse(String resp) {
        Crashlytics.log(android.util.Log.INFO, TAG, "err_resp: " + resp);
    }

    public static void logException(Throwable e) {
        Crashlytics.logException(e);
    }
}
