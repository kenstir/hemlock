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
import java.net.URI;
import java.net.URL;
import java.util.Map;

import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.auth.Const;
import org.opensrf.Method;
import org.opensrf.ShouldNotHappenException;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.util.JSONWriter;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static HttpURLConnection mConn = null;
    private static final int MAX_PARAMS = 5;
    private static String mLastAuthToken = null;

    /**
     * Gets the net page content.
     * 
     * @param url
     *            the url of the page to be retrieved
     * @return the net page content
     */
    public static String fetchUrl(String url) throws IOException {

        StringBuilder str = new StringBuilder();
        String line;
        if (mConn != null) mConn.disconnect();

        try {
            URL url2 = new URL(url);
            mConn = (HttpURLConnection) url2.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(mConn.getInputStream()));
            while ((line = in.readLine()) != null) {
                str.append(line);
            }
            in.close();
        } finally {
            if (mConn != null) mConn.disconnect();
        }

        return str.toString();
    }

    public static InputStream getNetInputStream(String url) throws IOException {

        if (mConn != null) mConn.disconnect();

        URL url2 = new URL(url);
        mConn = (HttpURLConnection) url2.openConnection();
        return mConn.getInputStream();
    }

    public static void closeNetInputStream() {
        if (mConn != null) {
            mConn.disconnect();
            mConn = null;
        }
    }

    public static String getResponseTextcode(Object response) {
        String textcode = null;
        try {
            textcode = ((Map<String, String>) response).get("textcode");
        } catch (Exception e) {
        }
        return textcode;
    }

    private static void logNPE(NullPointerException e, String service, String methodName) {
        // I know it's bad form to catch NPE.  But until I implement some kind of on-demand IDL parsing,
        // this is what happens when the JSONReader tries to parse a response of an unregistered class.
        // Crash if debugMode, fail if not.
        Crashlytics.log(android.util.Log.DEBUG, TAG, "NPE...unregistered type from service "+service+" method "+methodName);
        Crashlytics.logException(e);
        throw(e);
    }

    private static void logRequest(String service, String methodName, Object[] params) {
        Crashlytics.log(android.util.Log.DEBUG, TAG, "req: " + methodName);
        Crashlytics.setString("svc", service);
        Crashlytics.setString("m", methodName);
        if (params.length > MAX_PARAMS) {
            Log.d(TAG, "more params than expected");
        }
        int i;
        for (i = 0; i < params.length; i++) {
            String key = "p" + i;
            String val = "" + params[i];
            if (val.length() > 0 && TextUtils.equals(val, mLastAuthToken)) val = "***";//private
            Crashlytics.log(android.util.Log.DEBUG, TAG, " " + key + ": " + val);
            Crashlytics.setString(key, val);
        }
        for (; i < MAX_PARAMS; i++) {
            String key = "p" + i;
            Crashlytics.setString(key, null);
        }
    }

    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, String authToken,
                                   Object[] params) throws SessionNotFoundException {
        mLastAuthToken = authToken;
        logRequest(service, methodName, params);

        Method method = new Method(methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
        }

        // sync request
        long now_ms = System.currentTimeMillis();
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp = null;

        try {
            resp = req.recv();
        } catch (NullPointerException e) {
            logNPE(e, service, methodName);
        }
        Log.logElapsedTime(TAG, now_ms, "doRequest "+methodName);
        if (resp != null) {
            Crashlytics.log(android.util.Log.INFO, TAG, "resp: " + resp);
            Object response = (Object) resp;

            String textcode = getResponseTextcode(resp);
            if (TextUtils.equals(textcode, "NO_SESSION")) {
                Log.d(Const.AUTH_TAG, textcode);
                throw new SessionNotFoundException();
            }

            return response;
        }
        Crashlytics.logException(new ShouldNotHappenException(service, method));
        return null;

    }

    // alternate version of doRequest
    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, Object[] params) {
        logRequest(service, methodName, params);

        Method method = new Method(methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
        }

        // sync request
        long now_ms = System.currentTimeMillis();
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        try {
            while ((resp = req.recv()) != null) {
                Crashlytics.log(android.util.Log.INFO, TAG, "resp: " + resp);
                Object response = (Object) resp;

                Log.logElapsedTime(TAG, now_ms, "doRequest "+methodName);
                return response;
            }
        } catch (NullPointerException e) {
            logNPE(e, service, methodName);
        }
        Crashlytics.logException(new ShouldNotHappenException(service, method));
        return null;
    }

    public static String buildGatewayUrl(String service, String method, Object[] objects) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("/osrf-gateway-v1?service=").append(service);
        sb.append("&method=").append(method);
        URI uri = null;

        for (Object param : objects) {
            sb.append("&param=");
            sb.append(new JSONWriter(param).write());
        }

        try {
            // not using URLEncoder because it replaces ' ' with '+'.
            uri = new URI("http", "", null, sb.toString(), null);
        } catch (java.net.URISyntaxException ex) {
            Log.d(TAG, "caught", ex);
        }

        return uri.getRawQuery();
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
