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
import org.evergreen_ils.api.EvergreenService;
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

    private HttpConnection mConn = null;
    private ArrayList<SMSCarrier> mSMSCarriers = new ArrayList<>();
    private Boolean mIsSMSEnabled = null;

    private EvergreenServer() {
    }

    public static EvergreenServer getInstance() {
        if (mInstance == null)
            mInstance = new EvergreenServer();
        return mInstance;
    }

    public String getUrl() {
        return Gateway.baseUrl;
    }

    public String getUrl(String relativeUrl) {
        return Gateway.baseUrl + relativeUrl;
    }

    private void reset() {
        mConn = null;
        mSMSCarriers = new ArrayList<>();
        mIsSMSEnabled = null;
    }

    public HttpConnection gatewayConnection() {
        synchronized (this) {
            return mConn;
        }
    }

    private HttpURLConnection getURLConnection(String url) throws IOException {
        URL url2 = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
        conn.setUseCaches(false);
        return conn;
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
