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

import java.util.List;

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
    private static String mAuthToken;

    private static HttpConnection conn() { return mConn; }

    @BeforeClass
    public static void getAuthToken() throws Exception {
        // read username, password from extra options: -e username USER -e password PASS
        mContext = InstrumentationRegistry.getTargetContext();
        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server", "http://catalog.cwmars.org");
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
        List<OSRFObject> ccs_list = (List<OSRFObject>) Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_STATUS_ALL, new Object[] {});
        assertNotNull(ccs_list);
        assertTrue(ccs_list.size() > 0);
        Log.i(TAG, "ccs_list="+ccs_list);
    }

    @Test
    public void testOrgTypesRetrieve() throws Exception {
        assertLoggedIn();
        mConn = EvergreenServer.getInstance().gatewayConnection();
        List<OSRFObject> l = (List<OSRFObject>) Utils.doRequest(mConn, Api.ACTOR,
                Api.ORG_TYPES_RETRIEVE, new Object[] {});
        Log.i(TAG, "l="+l);
    }
}
