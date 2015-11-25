package org.evergreen_ils.auth;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

import android.text.TextUtils;
import android.util.Log;

public class EvergreenAuthenticator {
    private final static String TAG = EvergreenAuthenticator.class.getSimpleName();
    public final static String SERVICE_AUTH = "open-ils.auth";
    public final static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";
    public final static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";

    private static String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    // could use a for loop, but we're only dealing with a
                    // single byte
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "no MD5", e);
        }

        return "";
    }

    public static Object doRequest(HttpConnection conn, String service, String methodName, Object[] params) {
        Method method = new Method(methodName);

        Log.d(TAG, "doRequest> Method :" + methodName + ":");
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, "doRequest> Param " + i + ": " + params[i]);
        }

        // sync request
        HttpRequest req = new GatewayRequest(conn, service, method).send();
        Object resp;

        while ((resp = req.recv()) != null) {
            Log.d(TAG, "doRequest> Sync Response: " + resp);
            Object response = (Object) resp;
            return response;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static String signIn(String library_url, String username, String password) throws AuthenticationException {
        Log.d(TAG, "signIn> "+username+" "+library_url);

        HttpConnection conn;
        try {
            conn = new HttpConnection(library_url + "/osrf-gateway-v1");
        } catch (MalformedURLException e) {
            throw new AuthenticationException(e);
        }

        // step 1: get seed
        Object resp = doRequest(conn, SERVICE_AUTH, METHOD_AUTH_INIT, new Object[] { username });
        if (resp == null)
            throw new AuthenticationException("Unable to contact login service");
        String seed = resp.toString();

        // step 2: complete auth with seed + password
        HashMap<String, String> complexParam = new HashMap<String, String>();
        complexParam.put("type", "opac");
        complexParam.put("username", username);
        complexParam.put("password", md5(seed + md5(password)));
        resp = doRequest(conn, SERVICE_AUTH, METHOD_AUTH_COMPLETE, new Object[] { complexParam });
        if (resp == null)
            throw new AuthenticationException("Unable to complete login");
        
        // parse response
        String textcode = ((Map<String, String>) resp).get("textcode");
        Log.d(TAG, "textcode: " + textcode);
        if (textcode.equals("SUCCESS")) {
            Object payload = ((Map<String, String>) resp).get("payload");
            Log.d(TAG, "payload: " + payload);
            String authtoken = ((Map<String, String>) payload).get("authtoken");
            Log.d(TAG, "authtoken: " + authtoken);
            Integer authtime = ((Map<String, Integer>) payload).get("authtime");
            Log.d(TAG, "authtime: " + authtime);
            return authtoken;
        } else if (textcode.equals("LOGIN_FAILED")) {
            String desc = ((Map<String, String>) resp).get("desc");
            Log.d(TAG, "desc: "+desc);
            if (!TextUtils.isEmpty(desc)) {
                throw new AuthenticationException(desc);
            }
        }
        
        throw new AuthenticationException("Login failed");
    }
}
