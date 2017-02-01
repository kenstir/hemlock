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

import org.evergreen_ils.Api;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.auth.EvergreenAuthenticator;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.StdoutLogProvider;
import org.evergreen_ils.system.Utils;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.List;

/**
 * Created by kenstir on 1/29/2017.
 */
public class LoginTest {
    private static String TAG = LoginTest.class.getSimpleName();

    private static HttpConnection mConn;

    private static HttpConnection conn() { return mConn; }

    public static void main(String args[]) throws Exception {
        String server = "http://catalog.cwmars.org";
        String username = null;
        String password = null;

        if (args.length > 2) server = args[2];
        if (args.length > 1) {
            username = args[0];
            password = args[1];
        } else {
            System.err.println("usage: TestAny username password [server]");
            System.exit(1);
        }

        // sign in
        String auth_token = EvergreenAuthenticator.signIn(server, username, password);

        // init like the app does in LoadingTask
        EvergreenServer eg = EvergreenServer.getInstance();
        eg.connect(server);
        AccountAccess ac = AccountAccess.getInstance();

        // osrf is up
        mConn = new HttpConnection(server + "/osrf-gateway-v1");
        List<OSRFObject> ccs_list = (List<OSRFObject>) Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_STATUS_ALL, new Object[] {});
        Log.i(TAG, "ccs_list="+ccs_list);
    }
}
