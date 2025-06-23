/*
 * Copyright (C) 2022 Kenneth H. Cox
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.net;

import android.annotation.SuppressLint;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import net.kenstir.hemlock.android.Analytics;
import net.kenstir.hemlock.android.Log;
import net.kenstir.hemlock.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class GatewayStringRequest extends StringRequest {
    private final String TAG = GatewayStringRequest.class.getSimpleName();

    private final Priority mPriority;
    private final int mCacheTtlSeconds;
    protected Boolean mCacheHit;
    private final String mDebugTag;
    private final Long mStartTime;

    public GatewayStringRequest(String url, Priority priority, Response.Listener<String> listener, Response.ErrorListener errorListener, int cacheTtlSeconds) {
        super(Request.Method.GET, url, listener, errorListener);
        mPriority = priority;
        mCacheTtlSeconds = cacheTtlSeconds;
        mDebugTag = Integer.toHexString(url.hashCode());
        Log.d(TAG, String.format("[net] %1$8s send: %2$s", mDebugTag, url));
        Analytics.logRequest(mDebugTag, url);
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public Priority getPriority() {
        return mPriority;
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

    @SuppressLint("DefaultLocale")
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
