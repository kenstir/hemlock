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
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.evergreen_ils.android.Log;
import org.opensrf.util.GatewayResult;

import java.io.UnsupportedEncodingException;

public class GatewayJsonObjectRequest extends Request<GatewayResult> {
    private String TAG = GatewayJsonObjectRequest.class.getSimpleName();

    private final Response.Listener<GatewayResult> mListener;
    private final Priority mPriority;
    protected Boolean mCacheHit;

    public GatewayJsonObjectRequest(String url, Priority priority, Response.Listener<GatewayResult> listener, Response.ErrorListener errorListener) {
        super(Request.Method.GET, url, errorListener);
        Log.d(TAG, "[net] send "+url);
        this.mPriority = priority;
        this.mListener = listener;
    }

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

    protected Response<GatewayResult> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            Log.d(TAG, "[net] cached:"+(mCacheHit?"1":"0")+" url:"+getUrl());
            Log.d(TAG, "[net] recv "+response.data.length+": "+json);
            GatewayResult gatewayResult = GatewayResult.create(json);

            // don't cache failures
            Cache.Entry entry = gatewayResult.failed ? null : HttpHeaderParser.parseCacheHeaders(response);
            // TODO: limit duration of cache entry; server defaults to *one year*

            // treat all well-formed gatewayResults as success here, but don't cache failures
            return Response.success(gatewayResult, entry);
        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "caught", ex);
            return Response.error(new ParseError(ex));
        }
    }
}
