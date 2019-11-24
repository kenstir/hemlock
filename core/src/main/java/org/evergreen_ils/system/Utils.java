/*
 * Copyright (C) 2016 Kenneth H. Cox
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.text.TextUtils;

import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.net.Gateway;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static HttpURLConnection mConn = null;

    public static String getResponseTextcode(Object response) {
        String textcode = null;
        try {
            textcode = ((Map<String, String>) response).get("textcode");
        } catch (Exception e) {
        }
        return textcode;
    }

    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, String authToken,
                                   Object[] params) throws SessionNotFoundException {
        Method method = new Method(methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
        }
        Analytics.logRequest(service, method, authToken);

        // sync request
        long now_ms = System.currentTimeMillis();
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp = null;

        resp = req.recv();
        Analytics.logResponse(resp);
        Log.logElapsedTime(TAG, now_ms, "doRequest "+methodName);

        if (resp != null) {
            String textcode = getResponseTextcode(resp);
            if (TextUtils.equals(textcode, "NO_SESSION")) {
                Log.d(Const.AUTH_TAG, textcode);
                throw new SessionNotFoundException();
            }

            return resp;
        }
        return null;

    }

    // alternate version of doRequest
    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, Object[] params) {
        Method method = new Method(methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
        }
        Analytics.logRequest(service, method);

        // sync request
        long now_ms = System.currentTimeMillis();
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        while ((resp = req.recv()) != null) {
            Analytics.logResponse(resp);
            Log.logElapsedTime(TAG, now_ms, "doRequest "+methodName);
            return resp;
        }

        return null;
    }

    public static String safeString(String s) {
        if (s == null)
            return "";
        return s;
    }

    public static boolean safeBool(Boolean b) {
        if (b == null)
            return false;
        return b;
    }
}
