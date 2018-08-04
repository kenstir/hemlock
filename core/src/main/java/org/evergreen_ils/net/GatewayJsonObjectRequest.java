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

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.evergreen_ils.system.Log;
import org.opensrf.util.GatewayResponse;

import java.io.UnsupportedEncodingException;

/**
 * Created by kenstir on 12/13/2015.
 */
public class GatewayJsonObjectRequest extends Request<GatewayResponse> {
    private final Response.Listener<GatewayResponse> mListener;
    private final Priority mPriority;
    private String TAG = GatewayJsonObjectRequest.class.getSimpleName();

    public GatewayJsonObjectRequest(String url, Priority priority, Response.Listener<GatewayResponse> listener, Response.ErrorListener errorListener) {
        super(Request.Method.GET, url, errorListener);
        Log.d(TAG, "[net] send "+url);
        this.mPriority = priority;
        this.mListener = listener;
    }

    protected void deliverResponse(GatewayResponse response) {
        this.mListener.onResponse(response);
    }

    @Override
    public Priority getPriority() {
        return mPriority;
    }

    protected Response<GatewayResponse> parseNetworkResponse(NetworkResponse response) {
        GatewayResponse parsed;
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            Log.d(TAG, "[net] recv "+response.data.length+": "+json);
            parsed = GatewayResponse.create(json);
            if (parsed.failed == false) {
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            } else {
                Log.d(TAG, "parse failed", parsed.ex);
                return Response.error(new ParseError(parsed.ex));
            }
        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "caught", ex);
            return Response.error(new ParseError(ex));
        }
    }
}
