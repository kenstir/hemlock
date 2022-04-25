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

package org.evergreen_ils.test;

import android.content.Context;
import android.os.Bundle;
import androidx.test.platform.app.InstrumentationRegistry;

import android.text.TextUtils;

import org.evergreen_ils.Api;
import org.evergreen_ils.auth.EvergreenAuthenticator;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.android.Analytics;
import org.evergreen_ils.android.Log;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LiveServiceTest {
    private static final String TAG = LiveServiceTest.class.getSimpleName();

    private static Context mContext;
    private static HttpConnection mConn;
    private static String mServer;
    private static String mUsername;
    private static String mPassword;
    private static Integer mOrgID;
    private static String mAuthToken;

    private static HttpConnection conn() {
        return Gateway.INSTANCE.getConn();
    }

    @BeforeClass
    public static void getAuthToken() throws Exception {
        // See root build.gradle for how to override these vars (hint: secret.gradle)
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Analytics.initialize(mContext);
        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server");
        mOrgID = Integer.parseInt(b.getString("orgid", "1"));
        mUsername = b.getString("username");
        mPassword = b.getString("password");
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
            mAuthToken = Api.ANONYMOUS;
        } else {
            mAuthToken = EvergreenAuthenticator.signIn(mServer, mUsername, mPassword);
        }
        Log.d(TAG, "auth_token " + mAuthToken);
    }

    private static void assertLoggedIn() {
        assertTrue(!TextUtils.isEmpty(mAuthToken));
    }

    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testCopyStatusAll() throws Exception {
        assertLoggedIn();

        Object o = doRequest(conn(), Api.SEARCH, Api.COPY_STATUS_ALL, new Object[]{});
        List<OSRFObject> ccs_list = (List<OSRFObject>) o;
        assertNotNull(ccs_list);
        assertTrue(ccs_list.size() > 0);
        Log.i(TAG, "ccs_list=" + ccs_list);
    }

    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testOrgTypesRetrieve() throws Exception {
        assertLoggedIn();

        Object resp = doRequest(mConn, Api.ACTOR, Api.ORG_TYPES_RETRIEVE, new Object[]{});
        List<OSRFObject> l = (List<OSRFObject>) resp;
        Log.i(TAG, "l=" + l);
    }

    static void printMap(Map<String, ?> map) {
        List<String> keys = new ArrayList(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Log.d(TAG, key + " => " + map.get(key));
        }
    }

    // ORG_UNIT_SETTING_RETRIEVE - retrieve all settings from one org unit
    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testOrgUnitSettingsRetrieve() throws Exception {
        assertLoggedIn();

        Integer org_id = mOrgID;
        Object resp = doRequest(mConn, Api.ACTOR, Api.ORG_UNIT_SETTING_RETRIEVE, new Object[]{mAuthToken, org_id});
        Map<String, ?> resp_map = ((Map<String, ?>) resp);
        printMap(resp_map);
        Boolean is_pickup_location = null;
        is_pickup_location = !Api.parseBoolean(resp_map.get(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB));
        Log.d(TAG, "settingIsPickupLocation(" + org_id + ") = " + is_pickup_location);
    }

    // ORG_UNIT_SETTING_BATCH - retrieve a list of settings from one org unit
    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testOrgUnitSettingBatch() throws Exception {
        assertLoggedIn();

        Integer org_id = mOrgID;
        ArrayList<String> settings = new ArrayList<>();
        settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        settings.add(Api.SETTING_SMS_ENABLE);
        Object resp = doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING_BATCH, new Object[]{
                        org_id, settings, mAuthToken});
        Map<String, ?> resp_map = ((Map<String, ?>) resp);
        printMap(resp_map);
        Boolean is_pickup_location = null;
        Object o = resp_map.get(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        if (o != null) {
            Map<String, ?> resp_org_map = (Map<String, ?>) o;
            is_pickup_location = !Api.parseBoolean(resp_org_map.get("value"));
        }
        Log.d(TAG, "settingIsPickupLocation(" + org_id + ") = " + is_pickup_location);
    }

    // ORG_UNIT_SETTING - retrieve one setting from one org unit
    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testOrgUnitSetting() throws Exception {
        assertLoggedIn();

        Integer org_id = mOrgID;
        String setting = Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB;
        //String setting = Api.SETTING_SMS_ENABLE;
        Object resp = doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING, new Object[]{
                        org_id, setting, Api.ANONYMOUS});
        Boolean is_pickup_location = null;
        if (resp != null) {
            Map<String, ?> resp_map = ((Map<String, ?>) resp);
            printMap(resp_map);
            is_pickup_location = !Api.parseBoolean(resp_map.get("value"));
        }
        Log.d(TAG, "settingIsPickupLocation(" + org_id + ") = " + is_pickup_location);
    }

    @Test
    @Ignore("todo: reimpl service tests using mocks")
    public void testRetrieveSMSCarriers() throws Exception {
        assertLoggedIn();

        HashMap<String, Object> args = new HashMap<>();
        args.put("active", 1);
        Object resp = doRequest(conn(), Api.PCRUD,
                Api.SEARCH_SMS_CARRIERS, new Object[]{
                        Api.ANONYMOUS, args});
        Log.d(TAG, "did we make it?");
        if (resp != null) {
            ArrayList<OSRFObject> l = (ArrayList<OSRFObject>) resp;
            Log.d(TAG, "looks like we made it");
        }
    }

//    @Ignore("skip until I figure out how to pass username/password")
//    @Test
//    public void testMessagesRetrieve() throws Exception {
//        assertLoggedIn();
//
//        AccountAccess ac = AccountAccess.getInstance();
//
//        Integer unread_count = ac.getUnreadMessageCount();
//        assertNotNull(unread_count);
//        Log.d(TAG, "unread messages: " + unread_count);
//    }

    @Ignore("todo: reimpl service tests using mocks")
    @Test
    public void testSearchByOrgUnit() throws Exception {
        assertLoggedIn();

//        mConn = EvergreenServer.getInstance().gatewayConnection();
        //AccountAccess ac = AccountAccess.getInstance();
        //ac.retrieveSession(mAuthToken);

        String searchText = "harry potter chamber of secrets";
        String searchClass = "title";
        String searchFormat = "";
        String orgShortName = "ARL-ATH";

        HashMap argHash = new HashMap<String, Integer>();
        argHash.put("limit", 200);
        argHash.put("offset", 0);

        StringBuilder sb = new StringBuilder();
        sb.append(searchClass).append(":").append(searchText);
        if (!searchFormat.isEmpty())
            sb.append(" search_format(").append(searchFormat).append(")");
        if (orgShortName != null)
            sb.append(" site(").append(orgShortName).append(")");
        String queryString = sb.toString();

        long start_ms = System.currentTimeMillis();
        long now_ms = start_ms;

        // do request
        Object resp = doRequest(conn(), Api.SEARCH, Api.MULTICLASS_QUERY,
                new Object[]{argHash, queryString, 1});
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = Log.logElapsedTime(TAG, now_ms, "search.query");
        if (resp == null)
            return; // search failed or server crashed

        Map<String, ?> response = (Map<String, ?>) resp;
        Integer visible = Api.parseInt(response.get("count"), 0);

        // record_ids_lol is a list of lists and looks like one of:
        //   [[32673,null,"0.0"],[886843,null,"0.0"]] // integer ids+?
        //   [["503610",null,"0.0"],["502717",null,"0.0"]] // string ids+?
        //   [["1805532"],["2385399"]] // string ids only
        List<List<?>> record_ids_lol = (List<List<?>>) response.get("ids");
        Log.d(TAG, "length:" + record_ids_lol.size());
    }

    // hack until I evaluate the utility of this test code
    Object doRequest(HttpConnection conn, String service, String methodName, Object[] params) {
        return null;
    }
}
