package org.evergreen_ils.net;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.opensrf.util.GatewayResponse;

import java.io.UnsupportedEncodingException;

/**
 * Created by kenstir on 12/13/2015.
 */
public class GatewayJsonObjectRequest extends Request<GatewayResponse> {
    private final Response.Listener<GatewayResponse> mListener;

    public GatewayJsonObjectRequest(String url, Response.Listener<GatewayResponse> listener, Response.ErrorListener errorListener) {
        super(Request.Method.GET, url, errorListener);
        this.mListener = listener;
    }

    protected void deliverResponse(GatewayResponse response) {
        this.mListener.onResponse(response);
    }

    protected Response<GatewayResponse> parseNetworkResponse(NetworkResponse response) {
        GatewayResponse parsed;
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            parsed = GatewayResponse.create(json);
            if (parsed.failed == false) {
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            } else {
                return Response.error(new ParseError(parsed.ex));
            }
        } catch (UnsupportedEncodingException ex) {
            return Response.error(new ParseError(ex));
        }
    }
}
