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
package org.evergreen.android.globals;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.searchCatalog.Organisation;
import org.evergreen.android.searchCatalog.SearchCatalog;
import org.open_ils.idl.IDLParser;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GlobalConfigs {

    public static String IDL_FILE_FROM_ROOT = "/reports/fm_IDL.xml";
    public static String IDL_FILE_FROM_ASSETS = "fm_IDL.xml";
    public static String httpAddress = "";

    private boolean init = false;

    private static String TAG = "GlobalConfigs";
    
    private static boolean debugMode = true;//KCXXX make a developer preference

    public static boolean loadedIDL = false;

    public static boolean loadedOrgTree = false;

    public static String hold_icon_address = "/opac/images/tor/";

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

    private GlobalConfigs(Context context) {

        initialize(context);
    }

    public static GlobalConfigs getGlobalConfigs(Context context) {

        if (globalConfigSingleton == null) {
            globalConfigSingleton = new GlobalConfigs(context);
        }

        return globalConfigSingleton;
    }

    /*
     * Initialize function that retrieves IDL file and Orgs file
     */
    private boolean initialize(Context context) {
        if (!init) {
            loadIDLFile(context);
            getOrganisations();
            getCopyStatusesAvailable((ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE));
            init = true;
            return true;
        }
        return false;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean debugMode) {
        GlobalConfigs.debugMode = debugMode;
    }

    public void loadIDLFile(Context context) {

        try {
            Log.d(TAG, "loadIDLFile start");
            //@TODO maybe switch back to IDL_FILE_FROM_ROOT?class=circ&class=au&class=mvr&class=acp
            InputStream in_IDL = context.getAssets().open(IDL_FILE_FROM_ASSETS);
            IDLParser parser = new IDLParser(in_IDL);
            parser.setKeepIDLObjects(false);
            Log.d(TAG, "loadIDLFile parse");
            long start_ms = System.currentTimeMillis();
            parser.parse();
            long duration_ms = System.currentTimeMillis() - start_ms;
            Log.d(TAG, "loadIDLFile parse took "+duration_ms+"ms");
        } catch (Exception e) {
            Log.w(TAG, "Error in parsing IDL file", e);
        }

        loadedIDL = true;
    }

    /**
     * Fetch the OrgTree.js file, and from it parse the list of organisations.
     */
    public void getOrganisations() {

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
            /*
            for (int i=0; i<organisations.size(); ++i) {
                Log.d(TAG, "getOrg postsort org["+i+"]= id:"+organisations.get(i).id+" parent:"+organisations.get(i).parent+" name:"+organisations.get(i).name);
            }
            */

            long duration_ms = System.currentTimeMillis() - start_ms;
            Log.d("init", "Loading organisations took "+duration_ms+"ms");
            loadedOrgTree = true;
        }
    }

    public void getCopyStatusesAvailable(ConnectivityManager cm) {

        SearchCatalog search = SearchCatalog.getInstance(cm);

        try {
            search.getCopyStatuses();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void getOrgHiddentDepth() {

        // logic can be found in the opac_utils.js file in web/opac/common/js

        for (int i = 0; i < organisations.size(); i++) {
            AccountAccess ac = AccountAccess.getAccountAccess();
            try {
                Object obj = ac.fetchOrgSettings(organisations.get(i).id,
                        "opac.org_unit_hiding.depth");
            } catch (SessionNotFoundException e) {
            }

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
            //System.out.println(date);
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
            //System.out.println("Id " + organisations.get(i).id + " " + i);
            if (organisations.get(i).id == id)
                return organisations.get(i).name;
        }

        return null;
    }
}
