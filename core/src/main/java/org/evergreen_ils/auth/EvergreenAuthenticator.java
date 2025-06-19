package org.evergreen_ils.auth;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import net.kenstir.hemlock.data.evergreen.Api;
import net.kenstir.hemlock.android.Analytics;
import net.kenstir.hemlock.android.Log;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

import android.text.TextUtils;

public class EvergreenAuthenticator {
    private final static String TAG = EvergreenAuthenticator.class.getSimpleName();

    private static String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
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

    public static Object doRequest(HttpConnection conn, String service, String methodName, Object[] params) throws Exception {
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
            Object response = resp;
            return response;
        }

        if (req.failed()) {
            throw req.getFailure();
        }
        return null;
    }
    
    public static String signIn(String library_url, String username, String password) throws AuthenticationException {
        Analytics.log(TAG, "signIn: library_url=" + library_url);

        HttpConnection conn;
        try {
            conn = new HttpConnection(library_url + "/osrf-gateway-v1");
        } catch (MalformedURLException e) {
            throw new AuthenticationException(e);
        }

        // step 1: get seed
        Object resp = null;
        try {
            resp = doRequest(conn, Api.AUTH, Api.AUTH_INIT, new Object[]{username});
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }
        if (resp == null) {
            throw new AuthenticationException("Can't reach server at " + library_url +
                    "\n\nThe server may be offline.");
        }
        String seed = resp.toString();

        // step 2: complete auth with seed + password
        HashMap<String, String> param = new HashMap<>();
        param.put("type", "persist");// {opac|persist}, controls authtoken timeout
        param.put("username", username);
        param.put("password", md5(seed + md5(password)));
        try {
            resp = doRequest(conn, Api.AUTH, Api.AUTH_COMPLETE, new Object[]{param});
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }

        if (resp == null)
            throw new AuthenticationException("Unable to complete login");
        
        // parse response, throw if error
        // {"payload":[{"payload":{"authtoken":"***","authtime":1209600},"ilsevent":0,"textcode":"SUCCESS","desc":"Success"}],"status":200}
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
        } else if (!TextUtils.isEmpty(textcode)) {
            String desc = ((Map<String, String>) resp).get("desc");
            Log.d(TAG, "desc: " + desc);
            if (!TextUtils.isEmpty(desc)) {
                throw new AuthenticationException(desc);
            }
        }
        throw new AuthenticationException("Login failed");
    }
}
