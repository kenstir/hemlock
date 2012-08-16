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
package org.evergreen.android.accountAccess.holds;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.searchCatalog.RecordInfo;
import org.opensrf.util.OSRFObject;

public class HoldRecord implements Serializable {

    // metarecord
    public static final int M = 0;
    // record
    public static final int T = 1;
    // volume
    public static final int V = 2;
    // issuance
    public static final int I = 3;
    // copy
    public static final int C = 4;
    // part
    public static final int P = 5;

    private Integer requestLibID = null;

    private Integer pickupLibID = null;

    public Integer holdType = null;
    // id for target object
    public Integer target = null;
    public Date expire_time = null;

    public String title = null;

    public String author = null;

    public String types_of_resource;

    /*
     * Hold status holdStatus == 4 => AVAILABLE holdStatus == 3 => WAITING
     * holdStatus <= 3 => TRANSIT
     */

    // only for P types
    public String part_label = null;

    public Integer status = null;

    public Boolean active = null;

    // must also be serializable
    public OSRFObject ahr = null;
    // record info with more etails
    public RecordInfo recordInfo = null;

    public boolean email_notification = false;

    public boolean phone_notification = false;

    public boolean suspended = false;

    public Date thaw_date;

    public int pickup_lib;

    public HoldRecord(OSRFObject ahr) {

        this.target = ahr.getInt("target");
        String type = ahr.getString("hold_type");

        this.ahr = ahr;

        if (type.equals("M")) {
            holdType = M;
        } else if (type.equals("T")) {
            holdType = T;
        } else if (type.equals("V")) {
            holdType = V;
        } else if (type.equals("I")) {
            holdType = I;
        } else if (type.equals("C")) {
            holdType = C;
        } else if (type.equals("P"))
            holdType = P;

        this.expire_time = GlobalConfigs
                .parseDate(ahr.getString("expire_time"));

        this.thaw_date = GlobalConfigs.parseDate(ahr.getString("thaw_date"));
        String res = ahr.getString("email_notify");

        if (res.equals("t"))
            this.email_notification = true;
        res = ahr.getString("phone_notify");
        if (res.equals("t"))
            this.phone_notification = true;

        res = ahr.getString("frozen");
        if (res.equals("t"))
            this.suspended = true;
        pickup_lib = ahr.getInt("pickup_lib");

    }

    // based on status integer field retreive hold status in text
    public String getHoldStatus() {

        String holdStatus = "";

        if (holdType == 7)
            return "Suspended";
        if (holdType == 4)
            return "Available";
        if (holdType == 3)
            return "Waiting";
        if (holdType < 3)
            return "Transit";

        return holdStatus;
    }

}
