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

import org.evergreen_ils.data.SMSCarrier;
import org.evergreen_ils.net.Gateway;
import org.opensrf.net.http.HttpConnection;

import java.util.ArrayList;
import java.util.List;

public class EvergreenServer {

    private static final String TAG = EvergreenServer.class.getSimpleName();
    private static EvergreenServer mInstance = null;

    private HttpConnection mConn = null;
    private ArrayList<SMSCarrier> smsCarriers = new ArrayList<>();
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
        smsCarriers = new ArrayList<>();
        mIsSMSEnabled = null;
    }

    public HttpConnection gatewayConnection() {
        synchronized (this) {
            return mConn;
        }
    }

    public List<SMSCarrier> getSMSCarriers() {
        return smsCarriers;
    }

    public void setSMSEnabled(Boolean value) {
        mIsSMSEnabled = value;
    }

    public boolean getSMSEnabled() {
        return (mIsSMSEnabled != null) ? mIsSMSEnabled : false;
    }
}
