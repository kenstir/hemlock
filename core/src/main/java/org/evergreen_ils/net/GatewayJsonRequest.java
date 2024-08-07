/*
 * Copyright (C) 2015 Kenneth H. Cox
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
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.evergreen_ils.android.Analytics;
import org.evergreen_ils.android.Log;
import org.opensrf.util.GatewayResult;

import java.io.UnsupportedEncodingException;

public class GatewayJsonRequest extends Request<GatewayResult> {
    private final String TAG = GatewayJsonRequest.class.getSimpleName();

    private final Response.Listener<GatewayResult> mListener;
    private final Priority mPriority;
    private final int mCacheTtlSeconds;
    protected Boolean mCacheHit;
    private final String mDebugTag;

    public GatewayJsonRequest(String url, Priority priority, Response.Listener<GatewayResult> listener, Response.ErrorListener errorListener, int cacheTtlSeconds) {
        super(Request.Method.GET, url, errorListener);
        mPriority = priority;
        mListener = listener;
        mCacheTtlSeconds = cacheTtlSeconds;
        mDebugTag = Integer.toHexString(url.hashCode());
        Log.d(TAG, String.format("[net] %1$8s send: %2$s", mDebugTag, url));
        Analytics.logRequest(mDebugTag, url);
    }

    @Override
    protected void deliverResponse(GatewayResult response) {
        this.mListener.onResponse(response);
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
    protected Response<GatewayResult> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            Log.d(TAG, String.format("[net] %1$8s recv:%2$s %3$5d %4$s", mDebugTag, (mCacheHit?"*":" "), response.data.length, json));
            Analytics.logResponse(mDebugTag, getUrl(), mCacheHit, json);
            GatewayResult gatewayResult = GatewayResult.create(json);

            // decide whether to cache result
            Cache.Entry entry = (shouldCache() && gatewayResult.getShouldCache()) ? HttpHeaderParser.parseCacheHeaders(response) : null;

            // limit cache TTL
            if (entry != null && mCacheTtlSeconds > 0) {
                entry.softTtl = Math.min(entry.softTtl, entry.serverDate + mCacheTtlSeconds * 1000);
                entry.ttl = Math.min(entry.ttl, entry.serverDate + mCacheTtlSeconds * 1000);
            }

            // treat all well-formed gatewayResults as success
            return Response.success(gatewayResult, entry);
        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "caught", ex);
            return Response.error(new ParseError(ex));
        }
    }
}
