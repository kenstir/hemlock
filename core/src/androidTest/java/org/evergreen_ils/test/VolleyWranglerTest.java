package org.evergreen_ils.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.evergreen_ils.Api;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.system.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.opensrf.util.GatewayResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kenstir on 12/5/2015.
 */
@RunWith(AndroidJUnit4.class)
public class VolleyWranglerTest {

    private static final String TAG = VolleyWranglerTest.class.getSimpleName();
    private String mServer;
    private VolleyWrangler mVolley;
    private Response.ErrorListener mVolleyErrorListener;
    private Response.Listener<String> mVolleyStringResponseListener;
    private Response.Listener<JSONObject> mVolleyJsonResponseListener;
    private String mStringResponse = null;
    private JSONObject mJsonResponse = null;
    private String mError = null;
    private Exception mException = null;
    private long mStartTime;

    // this is how we block until all volley requests are finished
    private int mOutstandingRequests = 0;
    private final Lock mLock = new ReentrantLock();
    private final Condition mFinishedCondition = mLock.newCondition();

    @Before
    public void setUp() throws Exception {
        Context ctx = InstrumentationRegistry.getTargetContext();

        // read extra options: -e server SERVER
        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server", "http://catalog.cwmars.org");

        mVolley = VolleyWrangler.getInstance(ctx);
        mVolleyErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                long duration_ms = System.currentTimeMillis() - mStartTime;
                Log.d(TAG, "failure took " + duration_ms + "ms");
                mError = error.getMessage();
                finishRequest();
            }
        };
        mVolleyStringResponseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                long duration_ms = System.currentTimeMillis() - mStartTime;
                Log.d(TAG, "fetch took " + duration_ms + "ms");
                mStringResponse = response;
                Log.d(TAG, "response: " + response);
                finishRequest();
            }
        };
        mVolleyJsonResponseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                long duration_ms = System.currentTimeMillis() - mStartTime;
                Log.d(TAG, "fetch took " + duration_ms + "ms");
                mJsonResponse = response;
                Log.d(TAG, "response: " + response);
                finishRequest();
            }
        };
        mStartTime = System.currentTimeMillis();
    }

    private void startRequest(Request<?> request) {
        Log.d(TAG, "start:lock");
        mLock.lock();
        ++mOutstandingRequests;
        Log.d(TAG, "start:outstandingRequests=" + mOutstandingRequests);
        mVolley.addToRequestQueue(request);
        mLock.unlock();
    }

    private void finishRequest() {
        Log.d(TAG, "finish:lock");
        mLock.lock();
        --mOutstandingRequests;
        Log.d(TAG, "finish:outstandingRequests=" + mOutstandingRequests);
        if (mOutstandingRequests == 0)
            mFinishedCondition.signal();
        mLock.unlock();
    }

    private boolean waitForAllResponses() throws InterruptedException {
        Log.d(TAG, "waitForAllResponses:lock");
        mLock.lock();
        try {
            Log.d(TAG, "waitForAllResponses:await");
            return mFinishedCondition.await(1000, TimeUnit.MILLISECONDS);
        } finally {
            mLock.unlock();
        }
    }

    private String getUrl(String service, String method, Object[] objects) {
        String path = Utils.buildGatewayUrl(service, method, objects);
        return mServer + path;
    }

    @Test
    public void testVolleyFetch_basic() throws InterruptedException {
        String url = "https://evergreen-ils.org/directory/libraries.json";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                mVolleyStringResponseListener, mVolleyErrorListener);

        // volley and wait
        startRequest(request);
        waitForAllResponses();

        // assertions

        assertNotNull(mStringResponse);
    }

    @Test
    public void testVolley_json() throws Exception {
        String url = getUrl(Api.ACTOR, Api.ILS_VERSION, new Object[] {});
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                mVolleyJsonResponseListener, mVolleyErrorListener);

        // volley and wait
        startRequest(request);
        waitForAllResponses();

        // assertions

        assertNotNull(mJsonResponse);

        int status = mJsonResponse.getInt("status");
        Log.d(TAG, "status => " + status);
        assertEquals(200, status);

        JSONArray payload = mJsonResponse.getJSONArray("payload");
        Log.d(TAG, "payload => " + payload);
        assertEquals(1, payload.length());
        String version = payload.getString(0);
        assertNotNull(version);
    }

    @Test
    //todo replace with GatewayJsonObjectRequest
    public void testVolley_osrf() throws Exception {
        String url = getUrl(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, new Object[] {});
        StringRequest request = new StringRequest(Request.Method.GET, url,
                mVolleyStringResponseListener, mVolleyErrorListener);

        // volley and wait
        startRequest(request);
        waitForAllResponses();

        // assertions

        assertNotNull(mStringResponse);

        /* this code relies on having the IDL loaded
           so now we will punt and only do basic volley tests in this class
        GatewayResponse response = GatewayResponse.create(mStringResponse);

        assertNull(response.ex);

        assertNotNull(response.map);
        String status = response.map.get("status").toString();
        assertEquals("200", status);

        assertNotNull(response.responseList);
        Log.d(TAG, "responseList:" + response.responseList);
        String version = (String)response.responseList.remove(0);
        Log.d(TAG, "version:" + version);
        assertEquals(0, response.responseList.size());
          */
    }
}