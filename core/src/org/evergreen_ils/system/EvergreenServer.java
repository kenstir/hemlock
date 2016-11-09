/*
 * Copyright (C) 2016 Kenneth H. Cox
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
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import org.open_ils.idl.IDLException;
import org.open_ils.idl.IDLParser;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Represents the library system
 *
 * Created by kenstir on 11/7/2016.
 */
public class EvergreenServer {

    private static final String IDL_CLASSES_USED = "acn,acp,ahr,ahtc,aou,au,bmp,cbreb,cbrebi,cbrebin,cbrebn,ccs,circ,ex,mbt,mbts,mous,mra,mraf,mus,mvr,perm_ex";

    private static final String TAG = EvergreenServer.class.getSimpleName();
    private static EvergreenServer mInstance = null;

    private boolean mIsDebuggable = false;
    private String mUrl = null;
    private HttpConnection mConn = null;
    private boolean mIDLLoaded = false;
    private ArrayList<Organization> mOrganizations = null;
    private LinkedHashMap<String, String> mCopyStatuses = new LinkedHashMap<>();

    private EvergreenServer() {
    }

    public static EvergreenServer getInstance() {
        if (mInstance == null)
            mInstance = new EvergreenServer();
        return mInstance;
    }

    public boolean getIsDebuggable() {
        return mIsDebuggable;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getUrl(String relativeUrl) {
        return mUrl + relativeUrl;
    }

    public static String getIDLUrl(String library_url) {
        ArrayList<String> params = new ArrayList<String>(32);
        for (String className : TextUtils.split(IDL_CLASSES_USED, ",")) {
            params.add("class=" + className);
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append(library_url).append("/reports/fm_IDL.xml?");
        sb.append(TextUtils.join("&", params));
        return sb.toString();
    }

    public void enableCaching(Context context) {
        mIsDebuggable = (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(context.getCacheDir(), "volley");//try to reuse same cache dir as volley
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            Log.d(TAG, "HTTP response cache is unavailable.");
        }
    }

    private void reset() {
        mUrl = null;
        mConn = null;
        mIDLLoaded = false;
        mOrganizations = null;
    }

    public void connect(String library_url) throws IOException, IDLException {
        //library_url = "http://catalogx.cwmars.org"; ///HACK

        if (!TextUtils.equals(library_url, mUrl)) {
            reset();
            loadIDL(library_url);
            mConn = new HttpConnection(library_url + "/osrf-gateway-v1");
            mUrl = library_url;
        }
    }

    public HttpConnection gatewayConnection() {
        return mConn;
    }

    private void loadIDL(String library_url) throws IOException, IDLException {
        try {
            Log.d(TAG, "loadIDL.start");
            mIDLLoaded = false;
            long now_ms = System.currentTimeMillis();
            InputStream in_IDL = Utils.getNetInputStream(getIDLUrl(library_url));
            IDLParser parser = new IDLParser(in_IDL);
            parser.setKeepIDLObjects(false);
            now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL.init");
            parser.parse();
            now_ms = Log.logElapsedTime(TAG, now_ms, "loadIDL.total");
            mIDLLoaded = true;
        } finally {
            Utils.closeNetInputStream();
        }
    }

    public void addOrganization(OSRFObject obj, int level) {
        Organization org = new Organization();
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
            mOrganizations.add(org);

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
        mOrganizations = new ArrayList<Organization>();
        addOrganization(orgTree, 0);

        // If the org tree is too big, then an indented list is unwieldy.
        // Convert it into a flat list sorted by org.name.
        if (mOrganizations.size() > 25) {
            Collections.sort(mOrganizations, new Comparator<Organization>() {
                @Override
                public int compare(Organization a, Organization b) {
                    // top-level OU appears first
                    if (a.level == 0) return -1;
                    if (b.level == 0) return 1;
                    return a.name.compareTo(b.name);
                }
            });
            for (Organization o : mOrganizations) {
                o.indentedDisplayPrefix = "";
            }
        }
    }

    public ArrayList<Organization> getOrganizations() {
        return mOrganizations;
    }

    public Organization getOrganization(int id) {
        for (int i = 0; i < mOrganizations.size(); i++) {
            if (mOrganizations.get(i).id == id)
                return mOrganizations.get(i);
        }

        return null;
    }

    public String getOrganizationName(int id) {
        Organization org = getOrganization(id);
        if (org == null) {
            return null;
        } else {
            return org.name;
        }
    }

    public void loadCopyStatuses(List<OSRFObject> ccs_list) {
        mCopyStatuses.clear();
        for (OSRFObject ccs_obj: ccs_list) {
            if (ccs_obj.getString("opac_visible").equals("t")) {
                mCopyStatuses.put(ccs_obj.getInt("id") + "", ccs_obj.getString("name"));
                //Log.d(TAG, "Add status "+ccs_obj.getString("name"));
            }
        }
    }

    public Map<String, String> getCopyStatuses() {
        return mCopyStatuses;
    }
}
