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
