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
import org.evergreen_ils.android.App;
import org.evergreen_ils.api.EvergreenService;
import org.evergreen_ils.data.Account;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.opensrf.util.GatewayResult;
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

    public interface OnResponseListener<T> {
        public void onResponse(T data);
    }
    public interface OnErrorListener<T> {
        public void onError(String errorMessage);
    }

    private static final String TAG = EvergreenServerLoader.class.getSimpleName();

    private static int mOutstandingRequests = 0;
    private static long start_ms = 0;

    private static Boolean parseBoolSetting(GatewayResult response, String setting) {
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

    private static void parseOrgSettingsFromGatewayResponse(GatewayResult response, final Organization org) {
        Boolean not_pickup_lib = parseBoolSetting(response, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        if (not_pickup_lib != null)
            org.settingIsPickupLocation = !not_pickup_lib;
        Boolean allow_credit_payments = parseBoolSetting(response, Api.SETTING_CREDIT_PAYMENTS_ALLOW);
        if (allow_credit_payments != null)
            org.settingAllowCreditPayments = allow_credit_payments;
        Boolean sms_enable = parseBoolSetting(response, Api.SETTING_SMS_ENABLE);
        if (sms_enable != null)
            EvergreenServer.getInstance().setSMSEnabled(sms_enable);

        org.settingsLoaded = true;
    }

    // fetch settings that we need for all orgs
    public static void fetchOrgSettings(Context context) {
        /*
        startVolley();
        final EvergreenServer eg = EvergreenServer.getInstance();
        final AccountAccess ac = AccountAccess.getInstance();
        final Organization home_org = EvergreenService.Companion.findOrg(ac.getHomeLibraryID());
        final Organization pickup_org = EvergreenService.Companion.findOrg(ac.getDefaultPickupLibraryID());

        // To minimize risk of race condition, load home and pickup orgs first.
        // Use a clone so we don't screw up the search org spinner.
        // TODO: replace with coroutines
        ArrayList<Organization> orgs = new ArrayList<>();//(ArrayList<Organization>) EvergreenService.Companion.getOrgs().clone();
        if (home_org != null) {
            orgs.remove(home_org);
            orgs.add(0, home_org);
        }
        if (pickup_org != null) {
            orgs.remove(pickup_org);
            orgs.add(0, pickup_org);
        }

        for (final Organization org : orgs) {
            if (org.settingsLoaded)
                continue;
            ArrayList<String> settings = new ArrayList<>();
            settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
            settings.add(Api.SETTING_CREDIT_PAYMENTS_ALLOW);
            if (org.parent_ou == null) {
                settings.add((Api.SETTING_SMS_ENABLE));
            }
            final String method = Api.ORG_UNIT_SETTING_BATCH;
            String url = eg.getUrl(Analytics.buildGatewayUrl(
                    Api.ACTOR, method,
                    new Object[]{org.id, settings, ac.getAuthToken()}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.NORMAL,
                    new Response.Listener<GatewayResult>() {
                        @Override
                        public void onResponse(GatewayResult response) {
                            Analytics.logVolleyResponse(method);
                            parseOrgSettingsFromGatewayResponse(response, org);
                            decrNumOutstanding();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Analytics.logErrorResponse(error.getMessage());
                            String msg = error.getMessage();
                            if (!TextUtils.isEmpty(msg))
                                Log.d(TAG, "id=" + org.id + " error: " + msg);
                            decrNumOutstanding();
                        }
                    });
            incrNumOutstanding();
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
        */
    }

    private static void parseSMSCarriersFromGatewayResponse(GatewayResult response) {
        try {
            List<OSRFObject> resp_list = (List<OSRFObject>) response.payload;
            EvergreenServer.getInstance().loadSMSCarriers(resp_list);
        } catch (Exception ex) {
            Log.d(TAG, "caught", ex);
        }
    }

    public static void fetchSMSCarriers(Context context) {
        /*
        startVolley();
        final EvergreenServer eg = EvergreenServer.getInstance();
        final AccountAccess ac = AccountAccess.getInstance();
        HashMap<String, Object> args = new HashMap<>();
        args.put("active", 1);
        final String method = Api.SEARCH_SMS_CARRIERS;
        String url = eg.getUrl(Analytics.buildGatewayUrl(
                Api.PCRUD, method,
                new Object[]{ac.getAuthToken(), args}));
        GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                new Response.Listener<GatewayResult>() {
                    @Override
                    public void onResponse(GatewayResult response) {
                        Analytics.logVolleyResponse(method);
                        parseSMSCarriersFromGatewayResponse(response);
                        decrNumOutstanding();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Analytics.logErrorResponse(error.getMessage());
                        String msg = error.getMessage();
                        if (!TextUtils.isEmpty(msg))
                            Log.d(TAG, "error: "+msg);
                        decrNumOutstanding();
                    }
                });
        incrNumOutstanding();
        VolleyWrangler.getInstance(context).addToRequestQueue(r);
        */
    }

    private static Integer parseMessagesResponse(GatewayResult response) {
        Integer unread_count = 0;
        if (response.payload != null) {
            try {
                List<OSRFObject> list = (List<OSRFObject>) response.payload;
                for (OSRFObject obj : list) {
                    String read_date = obj.getString("read_date");
                    Boolean deleted = Api.parseBoolean(obj.get("deleted"));
                    if (read_date == null && !deleted) {
                        ++unread_count;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "caught", e);
            }
        }
        return unread_count;
    }

    /** fetch number of unread messages in patron message center
     *
     * We don't care about the messages themselves, because I don't see a way to modify
     * the messages via OSRF, and it's easier to launch a URL to the patron message center.
     */
    public static void fetchUnreadMessageCount(Context context, final OnResponseListener<Integer> listener) {
        startVolley();
        final EvergreenServer eg = EvergreenServer.getInstance();
        final Account account = App.getAccount();
        final String method = Api.MESSAGES_RETRIEVE;
        String url = eg.getUrl(Analytics.buildGatewayUrl(
                Api.ACTOR, method,
                new Object[]{account.getAuthToken(), account.getId(), null}));
        GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                new Response.Listener<GatewayResult>() {
                    @Override
                    public void onResponse(GatewayResult response) {
                        Analytics.logVolleyResponse(method);
                        listener.onResponse(parseMessagesResponse(response));
                        decrNumOutstanding();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Analytics.logErrorResponse(error.getMessage());
                        String msg = error.getMessage();
                        if (!TextUtils.isEmpty(msg))
                            Log.d(TAG, "error: "+msg);
                        decrNumOutstanding();
                    }
                });
        incrNumOutstanding();
        r.setShouldCache(false);
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
