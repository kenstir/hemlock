package org.evergreen_ils.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class EvergreenAuthenticate {
    private final static String TAG = "eg.auth";
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
            e.printStackTrace();
        }

        return "";
    }

    public static Object doRequest(HttpConnection conn, String service, String methodName, Object[] params) throws Exception {
        Method method = new Method(methodName);

        Log.d(TAG, "doRequest Method :" + methodName + ":");
        for (int i = 0; i < params.length; i++) {
            method.addParam(params[i]);
            Log.d(TAG, "Param " + i + ": " + params[i]);
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
    
    @SuppressWarnings("unchecked")
    public static String signIn(Context context, String username, String password) throws Exception {
        Log.d(TAG, "signIn "+username);

        HttpConnection conn = new HttpConnection(context.getString(R.string.gateway_url));

        // step 1: get seed
        Object resp = doRequest(conn, SERVICE_AUTH, METHOD_AUTH_INIT, new Object[] { username });
        if (resp == null)
            throw new Exception("Unable to contact login service");
        String seed = resp.toString();

        // step 2: complete auth with seed + password
        HashMap<String, String> complexParam = new HashMap<String, String>();
        complexParam.put("type", "opac");
        complexParam.put("username", username);
        complexParam.put("password", md5(seed + md5(password)));
        resp = doRequest(conn, SERVICE_AUTH, METHOD_AUTH_COMPLETE, new Object[] { complexParam });
        if (resp == null)
            throw new Exception("Unable to complete login");
        
        // parse response
        String textcode = ((Map<String, String>) resp).get("textcode");
        System.out.println("textcode: " + textcode);
        if (textcode.equals("SUCCESS")) {
            Object payload = ((Map<String, String>) resp).get("payload");
            System.out.println("payload: " + payload);
            String authtoken = ((Map<String, String>) payload).get("authtoken");
            System.out.println("authtoken: " + authtoken);
            Integer authtime = ((Map<String, Integer>) payload).get("authtime");
            System.out.println("authtime: " + authtime);
            return authtoken;
        } else if (textcode.equals("LOGIN_FAILED")) {
            String desc = ((Map<String, String>) resp).get("desc");
            System.out.println("desc: "+desc);
            if (!TextUtils.isEmpty(desc)) {
                throw new Exception(desc);
            }
        }
        
        throw new Exception("Login failed");
    }
}
