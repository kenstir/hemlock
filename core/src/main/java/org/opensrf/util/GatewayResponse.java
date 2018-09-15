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

import android.text.TextUtils;

import org.open_ils.Event;
import org.opensrf.ShouldNotHappenException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by kenstir on 12/5/2015.
 */
//TODO: reconcile this with org.evergreen_ils.Result, model after hemlock-ios
public class GatewayResponse {
    public List<Object> responseList = null;
    public Object payload = null;
    public String status = null;
    public Map<String, ?> map = null;
    public Exception ex = null;
    public boolean failed = false;
    public String description = null;

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
            resp.payload = (resp.responseList.size() > 0) ? resp.responseList.remove(0) : null;
        } catch (JSONException e) {
            resp.ex = e;
            resp.description = e.getMessage();
            resp.failed = true;
        }
        return resp;
    }

    // Really GatewayRequest.recv() should return this directly, but that is used everywhere and so
    // I am refactoring incrementally.
    //
    // payload_obj is returned from Utils.doRequest, and AFAIK it can be one of 3 things:
    // 1 - a map containing the response
    // 2 - an event (a map indicating an error)
    // 3 - a list of events
    public static GatewayResponse createFromObject(Object payload_obj) {
        GatewayResponse resp = new GatewayResponse();
        resp.payload = payload_obj;
        try {
            Event event = Event.parseEvent(payload_obj);
            if (event != null) {
                // single event
                resp.failed = event.failed();
                resp.description = event.getDescription();
                if (event.containsKey("payload"))
                    resp.map = (Map<String, ?>) event.get("payload");
            } else if (payload_obj instanceof ArrayList) {
                // list of events
                ArrayList<String> msgs = new ArrayList<>();
                for (Object obj: (ArrayList<Object>)payload_obj) {
                    event = Event.parseEvent(obj);
                    if (event != null) {
                        if (event.failed())
                            resp.failed = true;
                        msgs.add(event.getDescription());
                    }
                }
                resp.description = TextUtils.join("\n\n", msgs);
            } else if (payload_obj instanceof Map) {
                // response map
                resp.map = (Map<String, ?>) payload_obj;
            } else {
                resp.ex = new ShouldNotHappenException((payload_obj == null) ? "null response" : "Unexpected response");
                resp.description = resp.ex.getMessage();
                resp.failed = true;
            }
        } catch (Exception e) {
            resp.ex = e;
            resp.description = e.getMessage();
            resp.failed = true;
        }
        return resp;
    }
}
