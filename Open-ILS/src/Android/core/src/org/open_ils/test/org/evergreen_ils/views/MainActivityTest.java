package org.evergreen_ils.views;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.evergreen_ils.net.VolleyWrangler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kenstir on 12/5/2015.
 */
public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final String TAG = MainActivityTest.class.getName();
    private Activity mActivity;
    private VolleyWrangler mVolley;
    private String libraries_directory_url = "https://evergreen-ils.org/directory/libraries.json";
    private String mStringResponse = null;
    private String mError = null;
    private Exception mException = null;
    private final Lock mLock = new ReentrantLock();
    private final Condition mFinishedCondition = mLock.newCondition();
    private long mStartTime;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mVolley = VolleyWrangler.getInstance(mActivity);
        mStartTime = System.currentTimeMillis();
    }

    private void signalFinished() {
        mLock.lock();
        mFinishedCondition.signal();
        mLock.unlock();
    }

    private boolean waitForResponse() throws InterruptedException {
        mLock.lock();
        try {
            return mFinishedCondition.await(1000, TimeUnit.MILLISECONDS);
        } finally {
            mLock.unlock();
        }
    }

    private void handleResponse(String response) {
        long duration_ms = System.currentTimeMillis() - mStartTime;
        Log.d(TAG, "fetch took " + duration_ms + "ms");
        mStringResponse = response;
        Log.d(TAG, "response: " + response);
        signalFinished();
    }

    public void testVolleyFetch_basic() throws InterruptedException {
        RequestQueue q = VolleyWrangler.getInstance(mActivity).getRequestQueue();
        StringRequest request = new StringRequest(Request.Method.GET, libraries_directory_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mError = error.getMessage();
                    }
                });

        // submit the request and await the response
        q.add(request);
        waitForResponse();
        assertTrue(mStringResponse != null);
    }
}