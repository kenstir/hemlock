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

import android.text.TextUtils;

import org.evergreen_ils.Api;
import org.open_ils.idl.IDLException;
import org.open_ils.idl.IDLParser;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Represents the library system
 *
 * Created by kenstir on 11/7/2016.
 */
public class EvergreenServer {

    private static final String IDL_CLASSES_USED = "ac,acn,acp,ahr,ahtc,aou,aout,au,aua,auact,aum,aus,bmp,cbreb,cbrebi,cbrebin,cbrebn,ccs,circ,csc,cuat,ex,mbt,mbts,mous,mra,mraf,mus,mvr,perm_ex";

    private static final String TAG = EvergreenServer.class.getSimpleName();
    private static EvergreenServer mInstance = null;

    private String mUrl = null;
    private HttpConnection mConn = null;
    private boolean mIDLLoaded = false;
    private ArrayList<OrgType> mOrgTypes = null;
    private ArrayList<Organization> mOrganizations = null;
    private ArrayList<SMSCarrier> mSMSCarriers = null;
    private Boolean mIsSMSEnabled = null;
    private LinkedHashMap<String, String> mCopyStatuses = new LinkedHashMap<>();

    private EvergreenServer() {
    }

    public static EvergreenServer getInstance() {
        if (mInstance == null)
            mInstance = new EvergreenServer();
        return mInstance;
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

    private void reset() {
        mUrl = null;
        mConn = null;
        mIDLLoaded = false;
        mOrgTypes = null;
        mOrganizations = null;
        mSMSCarriers = null;
        mIsSMSEnabled = null;
    }

    public void connect(String library_url) throws IOException, IDLException {
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

    public void loadOrgTypes(List<OSRFObject> orgTypes) {
        mOrgTypes = new ArrayList<>();
        for (OSRFObject obj: orgTypes) {
            OrgType orgType = new OrgType();
            orgType.name = obj.getString("name");
            orgType.id = obj.getInt("id");
            orgType.opac_label = obj.getString("opac_label");
            orgType.can_have_users = Api.parseBoolean(obj.getString("can_have_users"));
            orgType.can_have_vols = Api.parseBoolean(obj.getString("can_have_vols"));
//            orgType.parent = obj.getInt("parent");
//            orgType.depth = obj.getInt("depth");
            mOrgTypes.add(orgType);
        }
    }

    private OrgType getOrgType(int id) {
        for (OrgType orgType: mOrgTypes) {
            if (orgType.id == id) {
                return orgType;
            }
        }
        return null;
    }

    public void addOrganization(OSRFObject obj, int level) {
        Organization org = new Organization();
        org.level = level;
        org.id = obj.getInt("id");
        org.parent_ou = obj.getInt("parent_ou");
        org.name = obj.getString("name");
        org.shortname = obj.getString("shortname");
        org.orgType = getOrgType(obj.getInt("ou_type"));
        org.opac_visible = Api.parseBoolean(obj.getString("opac_visible"));

        org.indentedDisplayPrefix = new String(new char[level]).replace("\0", "   ");
        Log.d(TAG, "id="+org.id+" level="+org.level+" type="+org.orgType.id+" users="+org.orgType.can_have_users+" vols="+org.orgType.can_have_vols+" vis="+(org.opac_visible ? "1" : "0")+" name="+org.name);

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

    public void loadOrganizations(OSRFObject orgTree, boolean hierarchical_org_tree) {
        mOrganizations = new ArrayList<>();
        addOrganization(orgTree, 0);

        // If the org tree is too big, then an indented list is unwieldy.
        // Convert it into a flat list sorted by org.name.
        if (!hierarchical_org_tree && mOrganizations.size() > 25) {
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
            return "";
        } else {
            return org.name;
        }
    }

    public void loadSMSCarriers(List<OSRFObject> carriers) {
        mSMSCarriers = new ArrayList<SMSCarrier>(carriers.size());
        for (OSRFObject obj : carriers) {
            SMSCarrier carrier = new SMSCarrier();
            carrier.id = obj.getInt("id");
            carrier.name = obj.getString("name");
            mSMSCarriers.add(carrier);
        }
        Collections.sort(mSMSCarriers, new Comparator<SMSCarrier>() {
            @Override
            public int compare(SMSCarrier a, SMSCarrier b) {
                return a.name.compareTo(b.name);
            }
        });
    }

    public List<SMSCarrier> getSMSCarriers() {
        return mSMSCarriers;
    }

    public void setSMSEnabled(Boolean value) {
        mIsSMSEnabled = value;
    }

    public boolean getSMSEnabled() {
        return (mIsSMSEnabled != null) ? mIsSMSEnabled : false;
    }

    public void loadCopyStatuses(List<OSRFObject> ccs_list) {
        mCopyStatuses.clear();
        for (OSRFObject ccs_obj: ccs_list) {
            if (Api.parseBoolean(ccs_obj.getString("opac_visible"))) {
                mCopyStatuses.put(ccs_obj.getInt("id") + "", ccs_obj.getString("name"));
                //Log.d(TAG, "Add status "+ccs_obj.getString("name"));
            }
        }
    }

    public Map<String, String> getCopyStatuses() {
        return mCopyStatuses;
    }
}
