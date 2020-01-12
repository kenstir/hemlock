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

import android.content.res.Resources;

import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.data.EgCodedValueMap;
import org.evergreen_ils.data.EgOrg;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.JsonUtils;
import org.evergreen_ils.utils.TextUtils;
import org.opensrf.util.OSRFObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HoldRecord implements Serializable {

    private static String TAG = HoldRecord.class.getSimpleName();

    public @NonNull OSRFObject ahr;
    public RecordInfo recordInfo = null;
    public @Nullable OSRFObject qstatsObj = null;

    // hold_type:
    //   T - title (default)
    //   C (or R or F?) - copy (requires staff client)
    //   I - issuance
    //   V - volume (requires staff client)
    //   M - meta-record
    //public String holdType = null;
    private String title = null;
    private String author = null;
    private String partLabel = null; // only for P types

    public HoldRecord(OSRFObject ahr) {
        this.ahr = ahr;
    }

    private String formatDateTime(Date date) {
        return DateFormat.getDateTimeInstance().format(date);
    }

    private String getTransitFrom() {
        OSRFObject transit = (OSRFObject) ahr.get("transit");
        if (transit == null) return null;
        Integer source = transit.getInt("source");
        if (source == null) return null;
        return EgOrg.getOrgNameSafe(source);
    }

    private String getTransitSince() {
        OSRFObject transit = (OSRFObject) ahr.get("transit");
        if (transit == null) return null;
        String sent = transit.getString("source_send_time");
        Date date = Api.parseDate(sent);
        return formatDateTime(date);
    }

    // Retrieve hold status in text
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
        Integer status = getStatus();
        if (status == null) {
            return "Status unavailable";
        } else if (status == 4) {
            String s = "Available";
            if (res.getBoolean(R.bool.ou_enable_hold_shelf_expiration) && getShelfExpireTime() != null)
                s = s + "\nExpires " + DateFormat.getDateInstance().format(getShelfExpireTime());
            return s;
        } else if (status == 7) {
            return "Suspended";
        } else if (getEstimatedWaitInSeconds() > 0) {
            int days = (int)Math.ceil((double) getEstimatedWaitInSeconds() / 86400.0);
            return "Estimated wait: "
                    + res.getQuantityString(R.plurals.number_of_days, days, days);
        } else if (status == 3 || status == 8) {
            return res.getString(R.string.hold_status_in_transit, getTransitFrom(), getTransitSince());
        } else if (status < 3) {
            String s = "Waiting for copy\n"
                    + res.getQuantityString(R.plurals.number_of_holds, getTotalHolds(), getTotalHolds()) + " on "
                    + res.getQuantityString(R.plurals.number_of_copies, getPotentialCopies(), getPotentialCopies());
            if (res.getBoolean(R.bool.ou_enable_hold_queue_position))
                s = s + "\n" + "Queue position: " + getQueuePosition();
            return s;
        } else {
            return "";
        }
    }

    public @NonNull String getHoldType() {
        String holdType = ahr.getString("hold_type");
        return (holdType != null) ? holdType : "?";
    }

    public static @NonNull List<HoldRecord> makeArray(@NonNull List<OSRFObject> ahr_objects) {
        ArrayList<HoldRecord> ret = new ArrayList<>();
        for (OSRFObject ahr_obj: ahr_objects) {
            ret.add(new HoldRecord(ahr_obj));
        }
        return ret;
    }

    private @NonNull String withPartLabel(@NonNull String title) {
        return (!TextUtils.isEmpty(partLabel)) ? title + " (" + partLabel + ")" : title;
    }

    public @NonNull String getTitle() {
        if (title != null && !TextUtils.isEmpty(title)) return withPartLabel(title);
        if (recordInfo != null && !TextUtils.isEmpty(recordInfo.title)) return withPartLabel(recordInfo.title);
        return "Unknown title";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        if (author != null && !TextUtils.isEmpty(author)) return author;
        if (recordInfo != null && !TextUtils.isEmpty(recordInfo.author)) return recordInfo.author;
        return "";
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Nullable
    public Date getExpireTime() {
        return ahr.getDate("expire_time");
    }

    @Nullable
    public Date getShelfExpireTime() {
        return ahr.getDate("shelf_expire_time");
    }

    @Nullable
    public Date getThawDate() {
        return ahr.getDate("thaw_date");
    }

    @Nullable
    public Integer getTarget() {
        return ahr.getInt("target");
    }

    public boolean isEmailNotify() {
        return ahr.getBoolean("email_notify");
    }

    public @Nullable String getPhoneNotify() {
        return ahr.getString("phone_notify");
    }

    public @Nullable String getSmsNotify() {
        return ahr.getString("sms_notify");
    }

    public boolean isSuspended() {
        return ahr.getBoolean("frozen");
    }

    public @Nullable Integer getPickupLib() {
        return ahr.getInt("pickup_lib");
    }

    public String getPartLabel() {
        return partLabel;
    }

    public void setPartLabel(String partLabel) {
        this.partLabel = partLabel;
    }

    public @Nullable Integer getStatus() {
        return (qstatsObj != null) ? qstatsObj.getInt("status") : null;
    }

    public @Nullable Integer getPotentialCopies() {
        return (qstatsObj != null) ? qstatsObj.getInt("potential_copies") : null;
    }

    public @Nullable Integer getEstimatedWaitInSeconds() {
        return (qstatsObj != null) ? qstatsObj.getInt("estimated_wait") : null;
    }

    public @Nullable Integer getQueuePosition() {
        return (qstatsObj != null) ? qstatsObj.getInt("queue_position") : null;
    }

    public @Nullable Integer getTotalHolds() {
        return (qstatsObj != null) ? qstatsObj.getInt("total_holds") : null;
    }

    public @Nullable String getFormatLabel() {
        if (getHoldType().equals("M")) {
            Map<String, Object> map = JsonUtils.parseObject(ahr.getString("holdable_formats"));
            List<String> labels = new ArrayList<>();
            for (String format: JsonUtils.parseHoldableFormats(map)) {
                labels.add(EgCodedValueMap.iconFormatLabel(format));
            }
            return android.text.TextUtils.join(" or ", labels);
        }
        return RecordInfo.getIconFormatLabel(recordInfo);
    }
}
