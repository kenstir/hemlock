package org.opensrf.util;

import java.util.List;
import java.util.Map;

/**
 * Created by kenstir on 12/5/2015.
 */
public class GatewayResponse {
    public List<Object> responseList = null;
    public Object payload = null;
    public String status = null;
    public Map<String, ?> map = null;
    public Exception ex = null;
    public boolean failed = false;

    private GatewayResponse() {
    }

    public static GatewayResponse create(String json) {
        GatewayResponse resp = new GatewayResponse();
        try {
            resp.map = (Map<String, ?>) new JSONReader(json).readObject();
            resp.status = resp.map.get("status").toString();
            if (!resp.status.equals("200"))
                resp.failed = true;
            resp.responseList = (List<Object>) resp.map.get("payload");
            resp.payload = resp.responseList.remove(0);
        } catch (JSONException e) {
            resp.ex = e;
            resp.failed = true;
        }
        return resp;
    }
}
