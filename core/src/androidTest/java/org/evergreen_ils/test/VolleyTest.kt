package org.evergreen_ils.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.Request
import com.android.volley.Response
import org.evergreen_ils.net.Gateway.buildUrl
import org.evergreen_ils.net.Gateway
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.JsonObjectRequest
import org.evergreen_ils.Api
import net.kenstir.hemlock.logging.Log
import org.evergreen_ils.net.Volley
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

private val TAG = VolleyTest::class.java.simpleName

/**
 * Created by kenstir on 12/5/2015.
 */
@RunWith(AndroidJUnit4::class)
class VolleyTest {
    private var mServer: String? = null
    private var mVolley: Volley? = null
    private var mVolleyErrorListener: Response.ErrorListener? = null
    private var mVolleyStringResponseListener: Response.Listener<String>? = null
    private var mVolleyJsonResponseListener: Response.Listener<JSONObject>? = null
    private var mStringResponse: String? = null
    private var mJsonResponse: JSONObject? = null
    private var mError: String? = null
    private var mStartTime: Long = 0

    // this is how we block until all volley requests are finished
    private var mOutstandingRequests = 0
    private val mLock: Lock = ReentrantLock()
    private val mFinishedCondition = mLock.newCondition()

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        // See root build.gradle for notes on customizing instrumented test variables
        val b = InstrumentationRegistry.getArguments()
        mServer = b.getString("server")
        Gateway.baseUrl = mServer!!
        Gateway.clientCacheKey = "42"
        mVolley = Volley.getInstance(ctx)
        mVolleyErrorListener = Response.ErrorListener { error ->
            val duration_ms = System.currentTimeMillis() - mStartTime
            Log.d(TAG, "failure took " + duration_ms + "ms")
            mError = error.message
            Log.d(TAG, "error: $mError")
            finishRequest()
        }
        mVolleyStringResponseListener = Response.Listener { response ->
            val duration_ms = System.currentTimeMillis() - mStartTime
            Log.d(TAG, "fetch took " + duration_ms + "ms")
            mStringResponse = response
            Log.d(TAG, "response: $response")
            finishRequest()
        }
        mVolleyJsonResponseListener = Response.Listener { response ->
            val duration_ms = System.currentTimeMillis() - mStartTime
            Log.d(TAG, "fetch took " + duration_ms + "ms")
            mJsonResponse = response
            Log.d(TAG, "response: $response")
            finishRequest()
        }
        mStartTime = System.currentTimeMillis()
    }

    private fun startRequest(request: Request<*>) {
        Log.d(TAG, "start:lock")
        mLock.lock()
        ++mOutstandingRequests
        Log.d(TAG, "start:outstandingRequests=$mOutstandingRequests")
        mVolley!!.addToRequestQueue(request)
        mLock.unlock()
    }

    private fun finishRequest() {
        Log.d(TAG, "finish:lock")
        mLock.lock()
        --mOutstandingRequests
        Log.d(TAG, "finish:outstandingRequests=$mOutstandingRequests")
        if (mOutstandingRequests == 0) mFinishedCondition.signal()
        mLock.unlock()
    }

    private fun waitForAllResponses(): Boolean {
        Log.d(TAG, "waitForAllResponses:lock")
        mLock.lock()
        return try {
            Log.d(TAG, "waitForAllResponses:await")
            mFinishedCondition.await(10000, TimeUnit.MILLISECONDS)
        } finally {
            mLock.unlock()
        }
    }

    private fun getUrl(service: String, method: String, objects: Array<Any?>): String {
        return buildUrl(service, method, objects)
    }

    @Test
    fun testVolleyFetch_basic() {
        val url = "https://evergreen-ils.org/directory/libraries.json"
        val request = StringRequest(Request.Method.GET, url,
            mVolleyStringResponseListener, mVolleyErrorListener)

        // volley and wait
        startRequest(request)
        waitForAllResponses()

        // assertions
        Assert.assertNotNull(mStringResponse)
    }

    @Test
    fun testVolley_json() {
        val url = getUrl(Api.ACTOR, Api.ILS_VERSION, arrayOf())
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            mVolleyJsonResponseListener, mVolleyErrorListener)

        // volley and wait
        startRequest(request)
        waitForAllResponses()

        // assertions
        Assert.assertNotNull(mJsonResponse)
        val status = mJsonResponse!!.getInt("status")
        Log.d(TAG, "status => $status")
        Assert.assertEquals(200, status.toLong())
        val payload = mJsonResponse!!.getJSONArray("payload")
        Log.d(TAG, "payload => $payload")
        Assert.assertEquals(1, payload.length().toLong())
        val version = payload.getString(0)
        Assert.assertNotNull(version)
    }

    @Test
    fun testVolley_osrf() {
        val url = getUrl(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, arrayOf())
        val request = StringRequest(Request.Method.GET, url,
            mVolleyStringResponseListener, mVolleyErrorListener)

        // volley and wait
        startRequest(request)
        waitForAllResponses()

        // assertions
        Assert.assertNotNull(mStringResponse)

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
