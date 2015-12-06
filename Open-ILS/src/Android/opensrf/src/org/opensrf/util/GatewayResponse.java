package org.opensrf.util;

import java.util.List;
import java.util.Map;

/**
 * Created by kenstir on 12/5/2015.
 */
public class GatewayResponse {
    public List<Object> responseList = null;
    public Map<String, ?> map = null;
    public Exception ex = null;

    private GatewayResponse() {
    }

    public static GatewayResponse create(String json) {
        GatewayResponse resp = new GatewayResponse();
        try {
            resp.map = (Map<String, ?>) new JSONReader(json).readObject();
            resp.responseList = (List<Object>) resp.map.get("payload");
        } catch (JSONException e) {
            resp.ex = e;
        }
        return resp;
    }
}
