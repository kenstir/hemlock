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
package org.evergreen_ils.accountAccess.checkout;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.evergreen_ils.Api;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.opensrf.util.OSRFObject;

/**
 * This is a wrapper class that get the information out for a circ object
 * 
 * @author daniel
 * 
 */
public class CircRecord {

    public enum CircType { OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED };

    public OSRFObject mvr = null;
    public OSRFObject acp = null;
    public OSRFObject circ = null;
    public RecordInfo recordInfo = null;
    public CircType circ_type;
    public int circ_id = -1;

    private Date circ_due_date = null;

    private Date parseDateFromField(OSRFObject obj, String field) {
        String s = obj.getString(field);
        if (s == null)
            return null;
        return Api.parseDate(s);
    }

    public CircRecord(OSRFObject circ, CircType circ_type, int circ_id) {
        this.circ = circ;
        this.circ_type = circ_type;
        this.circ_id = circ_id;
        this.circ_due_date = parseDateFromField(circ, "due_date");
    }

    public String getTitle() {
        if (this.recordInfo.title != null)
            return this.recordInfo.title;
        // 2016-07-06: not sure if these are ever needed
        String title;
        if (mvr != null) {
            title = mvr.getString("title");
            if (title != null) return title;
        }
        if (acp != null) {
            title = acp.getString("dummy_title");
            if (title != null) return title;
        }
        return "Unknown Title";
    }

    public String getAuthor() {
        String author;
        if (mvr != null) {
            author = mvr.getString("author");
            if (author != null) return author;
        }
        // 2019-02-19 I don't know if this code ever gets hit
        if (acp != null) {
            author = acp.getString("dummy_author");
            if (author != null) return author;
        }
        return "";
    }

    public String getDueDateString() {
        return DateFormat.getDateInstance().format(circ_due_date);
    }

    public Date getDueDate() {
        return circ_due_date;
    }

    public Integer getRenewals() {
        if (circ != null)
            return circ.getInt("renewal_remaining");

        return null;
    }

    public Integer getTargetCopy() {
        if (circ != null)
            return circ.getInt("target_copy");

        return null;
    }

    public boolean isOverdue() {
        Date currentDate = new Date();
        return getDueDate().compareTo(currentDate) < 0;
    }

    public boolean isDue() {
        Date currentDate = new Date();
        // Because the due dates in C/W MARS at least are 23:59:59, "3 days" here
        // really behaves like 2 days, highlighting if it's due tomorrow or the next day.
        final int ITEM_DUE_HIGHLIGHT_DAYS = 3;
        Calendar cal = Calendar.getInstance();
        cal.setTime(getDueDate());
        cal.add(Calendar.DAY_OF_MONTH, -ITEM_DUE_HIGHLIGHT_DAYS);
        return currentDate.compareTo(cal.getTime()) > 0;
    }
}
