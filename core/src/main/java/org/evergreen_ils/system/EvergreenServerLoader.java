/*
 * Copyright (C) 2017 Kenneth H. Cox
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

package org.evergreen_ils.system;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.evergreen_ils.Api;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Responsible for lazy loading of general server settings
 *
 * Created by kenstir on 2/20/2017.
 */
public class EvergreenServerLoader {
    private static final String TAG = EvergreenServerLoader.class.getSimpleName();

    private static int mOutstandingRequests = 0;
    private static long start_ms = 0;

    private static Boolean parseBoolSetting(GatewayResponse response, String setting) {
        Boolean value = null;
        try {
            Map<String, ?> resp_map = (Map<String, ?>) response.payload;
            Object o = resp_map.get(setting);
            if (o != null) {
                Map<String, ?> resp_setting_map = (Map<String, ?>)o;
                value = Api.parseBoolean(resp_setting_map.get("value"));
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        return value;
    }

    private static void parseSettingsFromGatewayResponse(GatewayResponse response, final Organization org) {
        Boolean not_pickup_lib = parseBoolSetting(response, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        if (not_pickup_lib != null)
            org.is_pickup_location = !not_pickup_lib;
        Boolean sms_enable = parseBoolSetting(response, Api.SETTING_SMS_ENABLE);
        if (sms_enable != null)
            EvergreenServer.getInstance().setSMSEnabled(sms_enable);
    }

    // fetch settings that we need for all orgs
    public static void fetchOrgSettings(Context context) {
        startVolley();
        int num_skipped = 0;
        final EvergreenServer eg = EvergreenServer.getInstance();
        final AccountAccess ac = AccountAccess.getInstance();
        for (final Organization org: eg.getOrganizations()) {
            ArrayList<String> settings = new ArrayList<>();
            settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
            if (org.parent_ou == null) {
                settings.add((Api.SETTING_SMS_ENABLE));
            } else if (!org.pickupLocationNeedsLoading()) {
                ++num_skipped;
                continue;
            }
            String url = eg.getUrl(Utils.buildGatewayUrl(
                    Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH,
                    new Object[]{org.id, settings, ac.getAuthToken()}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.NORMAL,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            parseSettingsFromGatewayResponse(response, org);
                            decrNumOutstanding();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String msg = error.getMessage();
                            if (!TextUtils.isEmpty(msg))
                                Log.d("kcxxx", "id="+org.id+" error: "+msg);
                            decrNumOutstanding();
                        }
                    });
            incrNumOutstanding();
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    private static void parseSMSCarriersFromGatewayResponse(GatewayResponse response) {
        Log.d(TAG, "response="+response);
        try {
            List<OSRFObject> resp_list = (List<OSRFObject>) response.payload;
            EvergreenServer.getInstance().loadSMSCarriers(resp_list);
        } catch (Exception ex) {
            Log.d(TAG, "caught", ex);
        }
    }

    public static void fetchSMSCarriers(Context context) {
        startVolley();
        final EvergreenServer eg = EvergreenServer.getInstance();
        final AccountAccess ac = AccountAccess.getInstance();
        HashMap<String, Object> args = new HashMap<>();
        args.put("active", 1);
        String url = eg.getUrl(Utils.buildGatewayUrl(
                Api.PCRUD_SERVICE, Api.SEARCH_SMS_CARRIERS,
                new Object[]{ac.getAuthToken(), args}));
        GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                new Response.Listener<GatewayResponse>() {
                    @Override
                    public void onResponse(GatewayResponse response) {
                        parseSMSCarriersFromGatewayResponse(response);
                        decrNumOutstanding();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String msg = error.getMessage();
                        if (!TextUtils.isEmpty(msg))
                            Log.d("kcxxx", "error: "+msg);
                        decrNumOutstanding();
                    }
                });
        incrNumOutstanding();
        VolleyWrangler.getInstance(context).addToRequestQueue(r);
    }

    // these don't really need to be synchronized as they happen on the main thread
    private static synchronized void startVolley() {
        if (mOutstandingRequests == 0) {
            start_ms = System.currentTimeMillis();
        }
    }
    private static synchronized void incrNumOutstanding() {
        ++mOutstandingRequests;
    }
    private static synchronized void decrNumOutstanding() {
        --mOutstandingRequests;
        if (mOutstandingRequests == 0) {
            Log.logElapsedTime(TAG, start_ms, "all requests finished");
            start_ms = 0;
        }
    }
}
