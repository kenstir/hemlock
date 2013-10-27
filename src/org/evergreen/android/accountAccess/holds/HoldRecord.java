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

import org.evergreen.android.R;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.searchCatalog.RecordInfo;
import org.opensrf.util.OSRFObject;
import org.simpleframework.xml.stream.Position;

import android.content.res.Resources;

public class HoldRecord implements Serializable {

    private Integer requestLibID = null;

    private Integer pickupLibID = null;

    public String holdType = null;
    // id for target object
    public Integer target = null;
    public Date expire_time = null;

    public String title = null;

    public String author = null;

    public String types_of_resource;

    // only for P types
    public String part_label = null;

    public Integer status = null;

    public Boolean active = null;

    // must also be serializable
    public OSRFObject ahr = null;
    // record info with more etails
    public RecordInfo recordInfo = null;

    public boolean email_notification = false;

    public String phone_notification = null;

    public boolean suspended = false;

    public Date thaw_date;

    public int pickup_lib;

    public Integer potentialCopies;

    public Integer estimatedWaitInSeconds;

    public Integer queuePosition;

    public Integer totalHolds;

    public HoldRecord(OSRFObject ahr) {

        this.target = ahr.getInt("target");
        this.holdType = ahr.getString("hold_type");

        this.ahr = ahr;

        this.expire_time = GlobalConfigs.parseDate(ahr.getString("expire_time"));
        this.thaw_date = GlobalConfigs.parseDate(ahr.getString("thaw_date"));
        this.email_notification = GlobalConfigs.parseBoolean(ahr.getString("email_notify"));
        this.phone_notification = ahr.getString("phone_notify");
        this.suspended = GlobalConfigs.parseBoolean(ahr.getString("frozen"));
        pickup_lib = ahr.getInt("pickup_lib");

    }

    // Retreive hold status in text
    public String getHoldStatus(Resources res) {
        // Constants from Holds.pm and logic from hold_status.tt2
        // -1 on error (for now),
        //  1 for 'waiting for copy to become available',
        //  2 for 'waiting for copy capture',
        //  3 for 'in transit',
        //  4 for 'arrived',
        //  5 for 'hold-shelf-delay'
        //  6 for 'canceled'
        //  7 for 'suspended'
        //  8 for 'captured, on wrong hold shelf'
        if (status == 4) {
            return "Available";
        } else if (estimatedWaitInSeconds > 0) {
            int days = (int)Math.ceil((double)estimatedWaitInSeconds / 86400.0);
            return "Estimated wait "+days+" day wait";
        } else if (status == 3 || status == 8) {
            return "In Transit";
        } else if (status < 3) {
            return "Waiting for copy\n"
                    + res.getQuantityString(R.plurals.number_of_holds, totalHolds, totalHolds) + " on "
                    + res.getQuantityString(R.plurals.number_of_copies, potentialCopies, potentialCopies)
                    + "\n"
                    + "Queue position: " + queuePosition;
        } else {
            return "";
        }
    }

}
