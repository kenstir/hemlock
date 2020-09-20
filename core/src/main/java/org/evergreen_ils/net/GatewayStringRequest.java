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

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import org.evergreen_ils.android.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class GatewayStringRequest extends StringRequest {
    private String TAG = GatewayStringRequest.class.getSimpleName();

    private final Priority mPriority;
    private final int mCacheTtlSeconds;
    protected Boolean mCacheHit;

    public GatewayStringRequest(String url, Priority priority, Response.Listener<String> listener, Response.ErrorListener errorListener, int cacheTtlSeconds) {
        super(Request.Method.GET, url, listener, errorListener);
        mPriority = priority;
        mCacheTtlSeconds = cacheTtlSeconds;
        Log.d(TAG, "[net] request "+url);
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

    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException ex) {
            parsed = new String(response.data, Charset.defaultCharset());
        }
        Log.d(TAG, "[net] cached:"+(mCacheHit?"1":"0")+" url:"+getUrl());
        Log.d(TAG, "[net] recv "+response.data.length+": "+parsed);

        // don't cache failures
        Cache.Entry entry = response.statusCode != 200 ? null : HttpHeaderParser.parseCacheHeaders(response);

        // limit cache TTL
        if (entry != null && mCacheTtlSeconds > 0) {
            entry.softTtl = Math.min(entry.softTtl, entry.serverDate + mCacheTtlSeconds * 1000);
            entry.ttl = Math.min(entry.ttl, entry.serverDate + mCacheTtlSeconds * 1000);
        }

        return Response.success(parsed, entry);
    }
}
