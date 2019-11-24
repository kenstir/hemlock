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
import org.evergreen_ils.net.Gateway;
import org.open_ils.idl.IDLException;
import org.open_ils.idl.IDLParser;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlin.NotImplementedError;

public class EvergreenServer {

    private static final String TAG = EvergreenServer.class.getSimpleName();
    private static EvergreenServer mInstance = null;

    private String mUrl = null;
    private HttpConnection mConn = null;
    private boolean mIDLLoaded = false;
    private ArrayList<OrgType> mOrgTypes = new ArrayList<>();
    private ArrayList<Organization> mOrganizations = new ArrayList<>();
    private ArrayList<SMSCarrier> mSMSCarriers = new ArrayList<>();
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

    private void reset() {
        mUrl = null;
        mConn = null;
        mIDLLoaded = false;
        mOrgTypes = null;
        mOrganizations = new ArrayList<>();
        mSMSCarriers = new ArrayList<>();
        mIsSMSEnabled = null;
    }

    public HttpConnection gatewayConnection() {
        return mConn;
    }

    private HttpURLConnection getURLConnection(String url) throws IOException {
        URL url2 = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
        conn.setUseCaches(false);
        return conn;
    }

    private OrgType getOrgType(int id) {
        for (OrgType orgType: mOrgTypes) {
            if (orgType.getId() == id) {
                return orgType;
            }
        }
        return null;
    }

    public ArrayList<Organization> getOrganizations() {
        return mOrganizations;
    }

    public Organization getOrganization(Integer id) {
        // ensure that getOrganization(null) returns null
        if (id != null) {
            for (Organization o : mOrganizations) {
                if (o.id.equals(id)) {
                    return o;
                }
            }
        }
        return null;
    }

    public String getOrganizationName(Integer id) {
        Organization org = getOrganization(id);
        if (org == null) {
            return "";
        } else {
            return org.name;
        }
    }

    public Organization getOrganizationByShortName(String orgShortName) {
        if (orgShortName != null) {
            for (Organization o : mOrganizations) {
                if (o.shortname.equals(orgShortName)) {
                    return o;
                }
            }
        }
        return null;
    }

    // Return the short names of the org itself and every level up to the consortium.
    // This is used to implement "located URIs".
    public ArrayList<String> getOrganizationAncestry(String orgShortName) {
        ArrayList<String> orgShortNames = new ArrayList<>();
        Organization org = getOrganizationByShortName(orgShortName);
        while (org != null) {
            orgShortNames.add(org.shortname);
            org = getOrganization(org.parent_ou);
        }
        return orgShortNames;
    }

    public String getOrganizationLibraryInfoPageUrl(Integer id) {
        Organization org = getOrganization(id);
        if (org == null) {
            return "";
        } else {
            // jump past the header stuff to the library info
            // #content-wrapper works only sometimes
            // ?#content-wrapper no better
            // /?#main-content no better
            // trying #main-content
            return getUrl("/eg/opac/library/" + org.shortname + "#main-content");
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
}
