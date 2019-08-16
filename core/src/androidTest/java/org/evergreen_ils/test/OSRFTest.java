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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import org.evergreen_ils.*;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.auth.EvergreenAuthenticator;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kenstir on 1/29/2017.
 */
@RunWith(AndroidJUnit4.class)
public class OSRFTest {
    private static String TAG = OSRFTest.class.getSimpleName();

    private static Context mContext;
    private static HttpConnection mConn;
    private static String mServer;
    private static String mUsername;
    private static String mPassword;
    private static Integer mOrgID;
    private static String mAuthToken;

    private static HttpConnection conn() { return mConn; }

    @BeforeClass
    public static void getAuthToken() throws Exception {
        // read extra options: -e server SERVER -e username USER -e password PASS
        mContext = InstrumentationRegistry.getTargetContext();
        Analytics.initialize(mContext);
        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server", "http://catalog.cwmars.org");
        mOrgID = Integer.parseInt(b.getString("orgid", "1"));
        mUsername = b.getString("username");
        mPassword = b.getString("password");
        // if username and password are empty, then maybe .idea/workspace.xml got messed up again;
        // it should contain something like:
        // <option name="EXTRA_OPTIONS" value="-e server http://gapines.org -e username USER -e password PASS" />
        // 2019-07-20: Looks like Android Studio 3.2.1 does not support EXTRA_OPTIONS any more,
        // manually entering it causes it to get wiped.  For now we will test as ANONYMOUS.
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
            mAuthToken = Api.ANONYMOUS;
        } else {
            mAuthToken = EvergreenAuthenticator.signIn(mServer, mUsername, mPassword);
        }
        Log.d(TAG, "auth_token "+mAuthToken);

        // init like the app does in LoadingTask
        EvergreenServer eg = EvergreenServer.getInstance();
        eg.connect(mServer);
        Log.d(TAG, "connected to "+mServer);
    }

    private static void assertLoggedIn() {
        assertTrue(!TextUtils.isEmpty(mAuthToken));
    }

    @Test
    public void testCopyStatusAll() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Object o = Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_STATUS_ALL, new Object[] {});
        List<OSRFObject> ccs_list = (List<OSRFObject>) o;
        assertNotNull(ccs_list);
        assertTrue(ccs_list.size() > 0);
        Log.i(TAG, "ccs_list="+ccs_list);
    }

    @Test
    public void testOrgTypesRetrieve() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Object resp = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_TYPES_RETRIEVE, new Object[] {});
        List<OSRFObject> l = (List<OSRFObject>) resp;
        Log.i(TAG, "l="+l);
    }

    // Api. ORG_UNIT_RETRIEVE is not useful, it returns the same info as ORG_TREE_RETRIEVE
    /*
    @Test
    public void testOrgUnitRetrieve() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Object o = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_RETRIEVE, new Object[] {
                        mAuthToken, mOrgID
                });
        Log.i(TAG, "o="+o);
    }
    */

    static void printMap(Map<String, ?> map) {
        List<String> keys = new ArrayList(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Log.d(TAG, key + " => " + map.get(key));
        }
    }

    // ORG_UNIT_SETTING_RETRIEVE - retrieve all settings from one org unit
    @Test
    public void testOrgUnitSettingsRetrieve() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Integer org_id = mOrgID;
        Object resp = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING_RETRIEVE, new Object[]{
                        mAuthToken, org_id});
        Map<String, ?> resp_map = ((Map<String, ?>) resp);
        printMap(resp_map);
        Boolean is_pickup_location = null;
        is_pickup_location = !Api.parseBoolean(resp_map.get(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB));
        Log.d(TAG, "setting_is_pickup_location("+org_id+") = "+is_pickup_location);
    }

    // ORG_UNIT_SETTING_BATCH - retrieve a list of settings from one org unit
    @Test
    public void testOrgUnitSettingBatch() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Integer org_id = mOrgID;
        ArrayList<String> settings = new ArrayList<>();
        settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        settings.add(Api.SETTING_SMS_ENABLE);
        Object resp = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING_BATCH, new Object[]{
                        org_id, settings, mAuthToken });
        Map<String, ?> resp_map = ((Map<String, ?>) resp);
        printMap(resp_map);
        Boolean is_pickup_location = null;
        Object o = resp_map.get(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        if (o != null) {
            Map<String, ?> resp_org_map = (Map<String, ?>) o;
            is_pickup_location = !Api.parseBoolean(resp_org_map.get("value"));
        }
        Log.d(TAG, "setting_is_pickup_location("+org_id+") = "+is_pickup_location);
    }

    // ORG_UNIT_SETTING - retrieve one setting from one org unit
    @Test
    public void testOrgUnitSetting() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Integer org_id = mOrgID;
        String setting = Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB;
        //String setting = Api.SETTING_SMS_ENABLE;
        Object resp = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING, new Object[]{
                        org_id, setting, Api.ANONYMOUS });
        Boolean is_pickup_location = null;
        if (resp != null) {
            Map<String, ?> resp_map = ((Map<String, ?>) resp);
            printMap(resp_map);
            is_pickup_location = !Api.parseBoolean(resp_map.get("value"));
        }
        Log.d(TAG, "setting_is_pickup_location("+org_id+") = "+is_pickup_location);
    }

    @Test
    public void testRetrieveSMSCarriers() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        HashMap<String, Object> args = new HashMap<>();
        args.put("active", 1);
        Object resp = Utils.doRequest(conn(), Api.PCRUD_SERVICE,
                Api.SEARCH_SMS_CARRIERS, new Object[] {
                        Api.ANONYMOUS, args});
        Log.d(TAG, "did we make it?");
        if (resp != null) {
            ArrayList<OSRFObject> l = (ArrayList<OSRFObject>) resp;
            Log.d(TAG, "looks like we made it");
        }
    }

    @Ignore("run on demand") @Test
    public void testCreateHold() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        AccountAccess ac = AccountAccess.getInstance();
        ac.retrieveSession(mAuthToken);

        // Test cases
        Integer recordID;
        //recordID = 3486408;//cwmars: Zero Theorem
        //recordID = 1;//cwmars: will fail
        //recordID = 4030530;//cwmars: Arrival (ITEM_AGE_PROTECTED)
        recordID = 3788817; //cwmars: Anomalisa (ok)

        Integer pickup_lib = ac.getDefaultPickupLibraryID();
        boolean email_notify = true;
        Integer sms_carrier_id = ac.getDefaultSMSCarrierID();
        String sms_number = ac.getDefaultSMSNumber();
        boolean suspendHold = false;
        String expire_time = null;
        String thaw_date = null;

        Result result = ac.testAndCreateHold(recordID, pickup_lib, email_notify, null,
                sms_number, sms_carrier_id, expire_time, suspendHold, thaw_date);
        Log.d(TAG, "ok=" + result.isSuccess());
        Log.d(TAG, "msg=" + result.getErrorMessage());
        Log.d(TAG, "here");
    }

    @Ignore("skip until I figure out how to pass username/password") @Test
    public void testMessagesRetrieve() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        AccountAccess ac = AccountAccess.getInstance();
        ac.retrieveSession(mAuthToken);

        Integer unread_count = ac.getUnreadMessageCount();
        assertNotNull(unread_count);
        Log.d(TAG, "unread messages: " + unread_count);
    }

    @Test
    public void testSearchByOrgUnit() throws Exception {
        assertLoggedIn();

        mConn = EvergreenServer.getInstance().gatewayConnection();
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
        Object resp = Utils.doRequest(conn(), Api.SEARCH, Api.MULTICLASS_QUERY,
                new Object[] { argHash, queryString, 1 });
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = Log.logElapsedTime(TAG, now_ms, "search.query");
        if (resp == null)
            return; // search failed or server crashed

        Map<String, ?> response = (Map<String, ?>) resp;
        Integer visible = Api.parseInteger(response.get("count"), 0);

        // record_ids_lol is a list of lists and looks like one of:
        //   [[32673,null,"0.0"],[886843,null,"0.0"]] // integer ids+?
        //   [["503610",null,"0.0"],["502717",null,"0.0"]] // string ids+?
        //   [["1805532"],["2385399"]] // string ids only
        List<List<?>> record_ids_lol = (List<List<?>>) response.get("ids");
        Log.d(TAG, "length:"+record_ids_lol.size());
    }
}
