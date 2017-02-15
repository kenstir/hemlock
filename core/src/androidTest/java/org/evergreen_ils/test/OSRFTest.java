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
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Collections;
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
        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server", "http://catalog.cwmars.org");
        mOrgID = Integer.parseInt(b.getString("orgid"));
        mUsername = b.getString("username");
        if (TextUtils.isEmpty(mUsername))
            return;
        mPassword = b.getString("password");
        if (TextUtils.isEmpty(mPassword))
            return;

        // sign in
        mAuthToken = EvergreenAuthenticator.signIn(mServer, mUsername, mPassword);
        if (TextUtils.isEmpty(mAuthToken))
            return;
        //assertNotEquals("non-empty auth token", "", mAuthToken);
        Log.d(TAG, "auth_token "+mAuthToken);

        // init like the app does in LoadingTask
        EvergreenServer eg = EvergreenServer.getInstance();
        eg.connect(mServer);
        Log.d(TAG, "connected to "+mServer);
        AccountAccess ac = AccountAccess.getInstance();
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
        Log.d(TAG, "is_pickup_location("+org_id+") = "+is_pickup_location);
    }

    // ORG_UNIT_SETTING_BATCH - retrieve a list of settings from one org unit
    @Test
    public void testOrgUnitSettingBatch() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        ArrayList<String> settings = new ArrayList<>();
        settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        Integer org_id = mOrgID;
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
        Log.d(TAG, "is_pickup_location("+org_id+") = "+is_pickup_location);
    }

    // ORG_UNIT_SETTING - retrieve one setting from one org unit
    @Test
    public void testOrgUnitSetting() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        Integer org_id = mOrgID;
        Object resp = Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_UNIT_SETTING, new Object[]{
                        org_id, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB, mAuthToken });
        Map<String, ?> resp_map = ((Map<String, ?>) resp);
        printMap(resp_map);
        Boolean is_pickup_location = null;
        if (resp_map != null) {
            is_pickup_location = !Api.parseBoolean(resp_map.get("value"));
        }
        Log.d(TAG, "is_pickup_location("+org_id+") = "+is_pickup_location);
    }
}
