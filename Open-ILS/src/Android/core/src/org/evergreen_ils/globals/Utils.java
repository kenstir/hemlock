/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.globals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import android.os.Looper;
import android.text.TextUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.ImageView;

public class Utils {
    private static final String TAG = "osrf";

    /**
     * Gets the net page content.
     * 
     * @param url
     *            the url of the page to be retrieved
     * @return the net page content
     */
    public static String getNetPageContent(String url) {

        String result = "";

        HttpResponse response = null;

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            response = client.execute(request);
        } catch (Exception e) {
            Log.d(TAG, "Exception to GET page " + url);
        }
        StringBuilder str = null;

        try {
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            str = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                str.append(line);
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error in retrieving response " + e.getMessage());
        }

        result = str.toString();

        return result;
    }

    public static InputStream getNetInputStream(String url) {

        InputStream in = null;

        HttpResponse response = null;

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            response = client.execute(request);
        } catch (Exception e) {
            Log.d(TAG, "Exception to GET page " + url);
        }

        try {
            in = response.getEntity().getContent();

            return in;
        } catch (Exception e) {
            System.err.println("Error in retrieving response " + e.getMessage());
        }

        return in;
    }

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

        Log.d(TAG, "doRequest Method " + methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, " param " + i + ":" + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp = null;

        try {
            resp = req.recv();
        } catch (NullPointerException e) {
            // I know it's bad form to catch NPE.  But until I implement some kind of on-demand IDL parsing,
            // this is what happens when the JSONReader tries to parse a response of an unregistered class.
            // Crash if debugMode, fail if not.
            Log.d(TAG, "NPE...unregistered type?", e);
            if (GlobalConfigs.isDebugMode()) {
                throw(e);
            }
        }
        if (resp != null) {
            Log.d(TAG, "Sync Response: " + resp);
            Object response = (Object) resp;

            String textcode = getResponseTextcode(resp);
            if (TextUtils.equals(textcode, "NO_SESSION")) {
                Log.d(TAG, textcode);
                throw new SessionNotFoundException();
            }

            return response;
        }
        return null;

    }

    // alternate version of doRequest
    // kcxxx: not sure why this one loops calling req.recv and the other doesn't
    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName,
                                   Object[] params) {

        Method method = new Method(methodName);

        Log.d(TAG, "doRequest Method " + methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, " param " + i + ": " + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        while ((resp = req.recv()) != null) {
            Log.d(TAG, "Sync Response: " + resp);
            Object response = (Object) resp;

            return response;

        }
        return null;

    }

    // does not throw exception
    // is fast than with checks for multiple method invocations like in search
    public static Object doRequestSimple(HttpConnection conn, String service,
            String methodName, Object[] params) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // running on UI thread!
            throw new NullPointerException();
        }

        Method method = new Method(methodName);
        Log.d(TAG, "doRequestSimple Method :" + methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, "Param " + i + ":" + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        while ((resp = req.recv()) != null) {
            Log.d(TAG, "Sync Response: " + resp);
            Object response = (Object) resp;

            return response;

        }
        return null;
    }

}
