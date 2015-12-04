/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.globals;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.searchCatalog.Organisation;
import org.evergreen_ils.searchCatalog.SearchCatalog;
import org.open_ils.idl.IDLException;
import org.open_ils.idl.IDLParser;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GlobalConfigs {

    public static String IDL_FILE_FROM_ROOT = "/reports/fm_IDL.xml?class=acn&class=acp&class=ahr&class=ahtc&class=aou&class=au&class=bmp&class=cbreb&class=cbrebi&class=cbrebin&class=cbrebn&class=ccs&class=circ&class=ex&class=mbt&class=mbts&class=mous&class=mra&class=mus&class=mvr&class=perm_ex";
    public static String IDL_FILE_FROM_ASSETS = "fm_IDL.xml";
    private static String httpAddress = "";
    private static HttpConnection conn = null;

    private static String TAG = "GlobalConfigs";
    
    private static Boolean isDebuggable = null;

    private static boolean loadedIDL = false;

    private static boolean loadedOrgTree = false;

    private static String hold_icon_address = "/opac/images/tor/";

    // two days notification before checkout expires, this can be modified from
    // preferences
    public static int NOTIFICATION_BEFORE_CHECKOUT_EXPIRATION = 2;

    // to parse date from requests
    public static final String datePattern = "yyyy-MM-dd'T'hh:mm:ssZ";

    /** The locale. */
    public String locale = "en-US";

    private static GlobalConfigs instance = null;

    /** The organisations. */
    public ArrayList<Organisation> organisations;

    /** The collections request. */
    private String collectionsRequest = "/opac/common/js/" + locale
            + "/OrgTree.js";

    private Context context = null;
    private long start_ms;

    private GlobalConfigs() {
    }

    public static GlobalConfigs getGlobalConfigs(Context context) {
        Log.d(TAG, "getGlobalConfigs (url="+httpAddress+")");
        if (instance == null)
            instance = new GlobalConfigs();
        if (context != null)
            instance.context = context;
        if (context != null && isDebuggable == null)
            isDebuggable = (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        return instance;
    }

    public static GlobalConfigs getGlobalConfigs(Context context, String library_url) {
        Log.d(TAG, "getGlobalConfigs library_url="+library_url);
        GlobalConfigs globalConfigs = getGlobalConfigs(context);
        globalConfigs.initialize(library_url);
        return globalConfigs;
    }

    public static String getUrl() {
        GlobalConfigs globalConfigs = getGlobalConfigs(null);
        return globalConfigs.httpAddress;
    }

    public static String getUrl(String relativeUrl) {
        GlobalConfigs globalConfigs = getGlobalConfigs(null);
        return globalConfigs.httpAddress + relativeUrl;
    }

    /*
     * Initialize function that retrieves IDL file and Orgs file
     */
    private boolean initialize(String library_url) {
        if (!TextUtils.equals(library_url, httpAddress)) {
            httpAddress = library_url;
            conn = null; // must come before loadXXX()
            loadIDL();
            loadCopyStatusesAvailable();
            return true;
        }
        return false;
    }

    public static boolean isDebuggable() {
        if (isDebuggable == null)
            return false;
        return isDebuggable;
    }

    public static HttpConnection gatewayConnection() {
        if (conn == null && !TextUtils.isEmpty(httpAddress)) {
            try {
                conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");
            } catch (MalformedURLException e) {
                Log.d(TAG, "unable to open connection", e);
            }
        }
        return conn;
    }

    public void loadIDL() {
        start_ms = System.currentTimeMillis();
        RequestQueue q = VolleyWrangler.getInstance(context).getRequestQueue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, httpAddress + IDL_FILE_FROM_ROOT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        long duration_ms = System.currentTimeMillis() - start_ms;
                        Log.d(TAG, "volley fetch took " + duration_ms + "ms");
                        InputStream in = new ByteArrayInputStream(response.getBytes());
                        IDLParser parser = new IDLParser(in);
                        parser.setKeepIDLObjects(false);
                        Log.d(TAG, "loadIDL parse");
                        try {
                            parser.parse();
                        } catch (Exception e) {
                            Log.d(TAG, "loadIDL failed", e);
                        }
                        duration_ms = System.currentTimeMillis() - start_ms;
                        Log.d(TAG, "loadIDL parse took "+duration_ms+"ms");
                    }
                },
                null);
        Log.d(TAG, "volley queuing request");
        q.add(stringRequest);
    }

    public void addOrganization(OSRFObject obj, int level) {
        Organisation org = new Organisation();
        org.level = level;
        org.id = obj.getInt("id");
        org.name = obj.getString("name");
        org.shortname = obj.getString("shortname");
        org.orgType = obj.getInt("ou_type");

        String opac_visible = obj.getString("opac_visible");
        org.opac_visible = TextUtils.equals(opac_visible, "t");

        org.indentedDisplayPrefix = new String(new char[level]).replace("\0", "  ");
        //Log.d(TAG, "kcxxx: id="+org.id+" level="+org.level+" name="+org.name+" vis="+(org.opac_visible ? "1" : "0"));
        if (org.opac_visible)
            organisations.add(org);

        List<OSRFObject> children = null;
        try {
            children = (List<OSRFObject>) obj.get("children");
            for (OSRFObject child : children) {
                addOrganization(child, level + 1);
            }
        } catch (Exception e) {
            Log.d(TAG, "addOrganization caught exception decoding children of "+org.name, e);
        }
    }

    public void loadOrganizations(OSRFObject orgTree) {
        organisations = new ArrayList<Organisation>();
        addOrganization(orgTree, 0);

        // If the org tree is too big, then an indented list is unwieldy.
        // Convert it into a flat list sorted by org.name.
        if (organisations.size() > 25) {
            Collections.sort(organisations, new Comparator<Organisation>() {
                @Override
                public int compare(Organisation a, Organisation b) {
                    // top-level OU appears first
                    if (a.level == 0) return -1;
                    if (b.level == 0) return 1;
                    return a.name.compareTo(b.name);
                }
            });
            for (Organisation o : organisations) {
                o.indentedDisplayPrefix = "";
            }
        }
    }

    public void loadCopyStatusesAvailable() {

        SearchCatalog search = SearchCatalog.getInstance();

        try {
            search.getCopyStatuses();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getStringDate(Date date) {

        final SimpleDateFormat sdf = new SimpleDateFormat(
                GlobalConfigs.datePattern);

        return sdf.format(date);

    }

    // parse from opac methods query results to Java date
    public static Date parseDate(String dateString) {

        if (dateString == null)
            return null;

        Date date = null;
        final SimpleDateFormat sdf = new SimpleDateFormat(
                GlobalConfigs.datePattern);

        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }
    
    // parse from opac methods query result to boolean
    public static boolean parseBoolean(String boolString) {
        return (boolString != null && boolString.equals("t"));
    }

    public String getOrganizationName(int id) {

        for (int i = 0; i < organisations.size(); i++) {
            if (organisations.get(i).id == id)
                return organisations.get(i).name;
        }

        return null;
    }
}
