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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import android.os.Looper;
import android.text.TextUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.auth.Const;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    /**
     * Gets the net page content.
     * 
     * @param url
     *            the url of the page to be retrieved
     * @return the net page content
     */
    public static String fetchUrl(String url) {

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

    private static void logNPE(NullPointerException e, String service, String methodName) {
        // I know it's bad form to catch NPE.  But until I implement some kind of on-demand IDL parsing,
        // this is what happens when the JSONReader tries to parse a response of an unregistered class.
        // Crash if debugMode, fail if not.
        Log.d(TAG, "NPE...unregistered type from service "+service+" method "+methodName, e);
        throw(e);
    }

    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, String authToken,
                                   Object[] params) throws SessionNotFoundException {

        Method method = new Method(methodName);

        Log.d(TAG, "doRequest Method " + methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, " param " + i + ": " + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp = null;

        try {
            resp = req.recv();
        } catch (NullPointerException e) {
            logNPE(e, service, methodName);
        }
        if (resp != null) {
            Log.d(TAG, "Sync Response: " + resp);
            Object response = (Object) resp;

            String textcode = getResponseTextcode(resp);
            if (TextUtils.equals(textcode, "NO_SESSION")) {
                Log.d(Const.AUTH_TAG, textcode);
                throw new SessionNotFoundException();
            }

            return response;
        }
        return null;

    }

    // alternate version of doRequest
    // kenstir todo: not sure why this one loops calling req.recv and the other doesn't
    public static Object doRequest(HttpConnection conn, String service,
                                   String methodName, Object[] params) {

        Method method = new Method(methodName);

        Log.d(TAG, "doRequest Method " + methodName);
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, " param " + i + ": " + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        try {
            while ((resp = req.recv()) != null) {
                Log.d(TAG, "Sync Response: " + resp);
                Object response = (Object) resp;

                return response;
            }
        } catch (NullPointerException e) {
            logNPE(e, service, methodName);
        }
        return null;
    }

    // does not throw exception
    // is fast than with checks for multiple method invocations like in search
    public static Object doRequestSimple(HttpConnection conn, String service,
            String method, Object[] params) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            // running on UI thread!
            throw new NullPointerException();
        }
        return doRequest(conn, service, method, params);
    }

}
