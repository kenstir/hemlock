/*
 * Copyright (c) 2023 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.net;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import net.kenstir.hemlock.android.Analytics;
import net.kenstir.hemlock.android.Log;
import org.evergreen_ils.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

// A request to an OPAC url that mimics a browser session with cookies.
// We need this for managing patron messages because there is no OSRF API for it.
public class OPACRequest extends StringRequest {
    private final String TAG = GatewayStringRequest.class.getSimpleName();

    private final Request.Priority mPriority;
    private final int mCacheTtlSeconds;
    protected Boolean mCacheHit;
    private final String mDebugTag;
    private final String mAuthToken;
    private final Long mStartTime;

    public OPACRequest(String url, String authToken, Request.Priority priority, Response.Listener<String> listener, Response.ErrorListener errorListener, int cacheTtlSeconds) {
        super(Request.Method.GET, url, listener, errorListener);
        mAuthToken = authToken;
        mPriority = priority;
        mCacheTtlSeconds = cacheTtlSeconds;
        mDebugTag = Integer.toHexString(url.hashCode());
        Log.d(TAG, String.format("[net] %1$8s send: %2$s", mDebugTag, url));
        Analytics.logRequest(mDebugTag, url);
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public Request.Priority getPriority() {
        return mPriority;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> m = new HashMap<>();
        String cookie = "ses=" + mAuthToken + "; eg_loggedin=1";
        m.put("Cookie", cookie);
        return m;
    }

    @Override
    public void addMarker(String tag) {
        super.addMarker(tag);
        if (tag.equals("cache-hit")){
            mCacheHit = true;
        } else if (tag.equals("network-http-complete")){
            mCacheHit = false;
        }
    }

    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        Analytics.logElapsed(TAG, mStartTime, mDebugTag, shouldCache(), "parseNetworkResponse");
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException ex) {
            parsed = new String(response.data, Charset.defaultCharset());
        }
        String trimmed = StringUtils.take(parsed, 512);
        Log.d(TAG, String.format("[net] %1$8s recv:%2$s %3$5d %4$s", mDebugTag, (mCacheHit?"*":" "), response.data.length, trimmed));
        Analytics.logResponse(mDebugTag, getUrl(), mCacheHit, trimmed);

        // decide whether to cache result
        Cache.Entry entry = (shouldCache() && response.statusCode == 200) ? HttpHeaderParser.parseCacheHeaders(response) : null;

        // limit cache TTL
        if (entry != null && mCacheTtlSeconds > 0) {
            entry.softTtl = Math.min(entry.softTtl, entry.serverDate + mCacheTtlSeconds * 1000);
            entry.ttl = Math.min(entry.ttl, entry.serverDate + mCacheTtlSeconds * 1000);
        }

        return Response.success(parsed, entry);
    }
}
