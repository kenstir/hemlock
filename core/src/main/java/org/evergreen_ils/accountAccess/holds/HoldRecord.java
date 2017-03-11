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
package org.evergreen_ils.accountAccess.holds;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.opensrf.util.OSRFObject;
import android.content.res.Resources;

public class HoldRecord implements Serializable {

    private static String TAG = HoldRecord.class.getSimpleName();

    private Integer requestLibID = null;
    private Integer pickupLibID = null;
    public String holdType = null;
    public Integer target = null;
    public Date expire_time = null;
    public String title = null;
    public String author = null;

    // only for P types
    public String part_label = null;

    public Integer status = null;

    public Boolean active = null;

    // must also be serializable
    public OSRFObject ahr = null;
    // record info with more etails
    public RecordInfo recordInfo = null;

    public boolean email_notify = false;

    public String phone_notify = null;
    public String sms_notify = null;

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

        this.expire_time = Api.parseDate(ahr.getString("expire_time"));
        this.thaw_date = Api.parseDate(ahr.getString("thaw_date"));
        this.email_notify = Api.parseBoolean(ahr.getString("email_notify"));
        this.phone_notify = ahr.getString("phone_notify");
        this.sms_notify = ahr.getString("sms_notify");
        this.suspended = Api.parseBoolean(ahr.getString("frozen"));
        pickup_lib = ahr.getInt("pickup_lib");
    }

    private String formatDateTime(Date date) {
        return DateFormat.getDateTimeInstance().format(date);
    }

    /*
     * Notes about transits:
     * - transit "prev_dest" could be the string "null"
     * - or prev_dest could be an org ID that is not OPAC visible like 68 Paxton
     */

    private String getTransitFrom() {
        try {
            OSRFObject transit = (OSRFObject) ahr.get("transit");
            if (transit == null) return null;
            EvergreenServer eg = EvergreenServer.getInstance();
            Integer source = transit.getInt("source");
            if (source == null) return null;
            return eg.getOrganizationName(source);
        } catch (Exception ex) {
            Log.d(TAG, "caught", ex);
            return null;
        }
    }
    private String getTransitSince() {
        try {
            OSRFObject transit = (OSRFObject) ahr.get("transit");
            if (transit == null) return null;
            String sent = transit.getString("source_send_time");
            Date date = Api.parseDate(sent);
            return formatDateTime(date);
        } catch (Exception ex) {
            Log.d(TAG, "caught", ex);
            return null;
        }
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
        } else if (status == 7) {
            return "Suspended";
        } else if (estimatedWaitInSeconds > 0) {
            int days = (int)Math.ceil((double)estimatedWaitInSeconds / 86400.0);
            return "Estimated wait: "
                    + res.getQuantityString(R.plurals.number_of_days, days, days);
        } else if (status == 3 || status == 8) {
            return res.getString(R.string.hold_status_in_transit, getTransitFrom(), getTransitSince());
        } else if (status < 3) {
            String status = "Waiting for copy\n"
                    + res.getQuantityString(R.plurals.number_of_holds, totalHolds, totalHolds) + " on "
                    + res.getQuantityString(R.plurals.number_of_copies, potentialCopies, potentialCopies);
            if (res.getBoolean(R.bool.ou_enable_hold_queue_position))
                status = status + "\n" + "Queue position: " + queuePosition;
            return status;
        } else {
            return "";
        }
    }

}
