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
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import org.evergreen_ils.searchCatalog.Organisation;
import org.evergreen_ils.searchCatalog.SearchCatalog;
import org.open_ils.idl.IDLParser;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GlobalConfigs {

    public static String IDL_FILE_FROM_ROOT = "/reports/fm_IDL.xml?class=acn&class=acp&class=ahr&class=ahtc&class=au&class=bmp&class=cbreb&class=cbrebi&class=cbrebin&class=cbrebn&class=ccs&class=circ&class=ex&class=mbt&class=mbts&class=mous&class=mra&class=mus&class=mvr&class=perm_ex";
    public static String IDL_FILE_FROM_ASSETS = "fm_IDL.xml";
    private static String httpAddress = "";
    private static HttpConnection conn = null;

    private static String TAG = "GlobalConfigs";
    
    private static boolean debugMode = true;//todo get from boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );

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

    private static GlobalConfigs globalConfigSingleton = null;

    /** The organisations. */
    public ArrayList<Organisation> organisations;

    /** The collections request. */
    private String collectionsRequest = "/opac/common/js/" + locale
            + "/OrgTree.js";

    private GlobalConfigs() {
    }

    public static GlobalConfigs getGlobalConfigs(Context context) {
        Log.d(TAG, "getGlobalConfigs (url="+httpAddress+")");
        if (globalConfigSingleton == null)
            globalConfigSingleton = new GlobalConfigs();
        return globalConfigSingleton;
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
            loadIDL();
            loadOrganizations();
            loadCopyStatusesAvailable();
            return true;
        }
        return false;
    }

    public static boolean isDebugMode() {
        return debugMode;
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

        try {
            Log.d(TAG, "loadIDL fetching " + httpAddress + IDL_FILE_FROM_ROOT);
            long start_ms = System.currentTimeMillis();
            InputStream in_IDL = Utils.getNetInputStream(GlobalConfigs.httpAddress + IDL_FILE_FROM_ROOT);
            IDLParser parser = new IDLParser(in_IDL);
            parser.setKeepIDLObjects(false);
            Log.d(TAG, "loadIDL parse");
            parser.parse();
            long duration_ms = System.currentTimeMillis() - start_ms;
            Log.d(TAG, "loadIDL parse took "+duration_ms+"ms");
        } catch (Exception e) {
            Log.w(TAG, "loadIDL parse error", e);
        }

        loadedIDL = true;
    }

    /**
     * Fetch the OrgTree.js file, and from it parse the list of organisations.
     */
    public void loadOrganizations() {

        String orgFile = null;

        organisations = new ArrayList<Organisation>();

        try {
            Log.d(TAG, "getOrg fetching "+httpAddress + collectionsRequest);
            orgFile = Utils.getNetPageContent(httpAddress + collectionsRequest);
        } catch (Exception e) {
        }

        if (orgFile != null) {
            long start_ms = System.currentTimeMillis();
            Log.d(TAG, "getOrg loading");
            organisations = new ArrayList<Organisation>();

            // in case of wrong file
            if (orgFile.indexOf("=") == -1)
                return;
            String orgArray = orgFile.substring(orgFile.indexOf("=") + 1,
                    orgFile.indexOf(";"));
            Log.d(TAG, "getOrg array=" + orgArray.substring(0, orgArray.length()>50 ? 50 : -1));

            // Parse javascript list
            // Format: [[id, ou_type, parent_ou, name, opac_visible, shortname],...]
            // Sample: [[149,3,146,"Agawam",0,"AGAWAM_MA"],
            //          [150,4,149,"Agawam Public Library",1,"AGAWAM"],...]
            // ou_type can be treated as hierarchical nesting level
            List orgList;
            try {
                orgList = new JSONReader(orgArray).readArray();
            } catch (JSONException e) {
                Log.d(TAG, "getOrg failed parsing array", e);
                return;
            }

            // Convert json list into array of Organisation
            for (int i=0; i<orgList.size(); ++i) {
                Organisation org = new Organisation();
                List orgItem = (List) orgList.get(i);
                org.id= (Integer)orgItem.get(0);
                org.level = (Integer)orgItem.get(1);
                org.parent = (Integer)orgItem.get(2);
                org.name = (String)orgItem.get(3);
                org.isVisible = (Integer)orgItem.get(4);
                org.shortName = (String)orgItem.get(5);
                if (org.isVisible == 0) {
                    continue;
                }

                organisations.add(org);
            }
            
            /*
            for (int i=0; i<organisations.size(); ++i) {
                Log.d(TAG, "getOrg presort org["+i+"]= id:"+organisations.get(i).id+" parent:"+organisations.get(i).parent+" name:"+organisations.get(i).name);
            }
            */
            Collections.sort(organisations, new Comparator<Organisation>() {
                @Override
                public int compare(Organisation a, Organisation b) {
                    if (a.parent == null)
                        return -1; // root is always first
                    return a.name.compareTo(b.name);
                }
            });

            long duration_ms = System.currentTimeMillis() - start_ms;
            Log.d(TAG, "getOrg took "+duration_ms+"ms");
            loadedOrgTree = true;
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
