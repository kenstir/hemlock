package org.evergreen_ils.views;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.net.VolleyWrangler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opensrf.util.GatewayResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kenstir on 12/5/2015.
 */
public class VolleyWranglerTest
        extends ActivityInstrumentationTestCase2<SimpleTestableActivity> {

    private static final String TAG = VolleyWranglerTest.class.getSimpleName();
    private Activity mActivity;
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

    public VolleyWranglerTest() {
        super(SimpleTestableActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mVolley = VolleyWrangler.getInstance(mActivity);
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
        RequestQueue q = VolleyWrangler.getInstance(mActivity).getRequestQueue();
        q.add(request);
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

    public void testVolley_json() throws Exception {
        String url = "http://bark.cwmars.org/osrf-gateway-v1?service=open-ils.actor&method=opensrf.open-ils.system.ils_version";
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

    public void testVolley_osrf() throws Exception {
        String url = "http://bark.cwmars.org/osrf-gateway-v1?service=open-ils.actor&method=opensrf.open-ils.system.ils_version";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                mVolleyStringResponseListener, mVolleyErrorListener);

        // volley and wait
        startRequest(request);
        waitForAllResponses();

        // assertions

        assertNotNull(mStringResponse);

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
    }
}