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
import org.evergreen_ils.utils.TextUtils;
import org.opensrf.util.OSRFObject;

public class CircRecord {

    public enum CircType { OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED }

    private int circId = -1;
    public OSRFObject circ;
    public OSRFObject mvr = null;
    public OSRFObject acp = null;
    public RecordInfo recordInfo = null;
    private CircType circType;

    private Date dueDate = null;

    public CircRecord(OSRFObject circ, CircType circType, int circId) {
        this.circ = circ;
        this.circType = circType;
        this.circId = circId;
        this.dueDate = circ.getDate("due_date");
    }

    // dummy_title is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    public String getTitle() {
        if (!TextUtils.isEmpty(this.recordInfo.title))
            return this.recordInfo.title;
        String title;
        if (mvr != null) {
            title = mvr.getString("title");
            if (!TextUtils.isEmpty(title)) return title;
        }
        if (acp != null) {
            title = acp.getString("dummy_title");
            if (!TextUtils.isEmpty(title)) return title;
        }
        return "Unknown Title";
    }

    // dummy_author is used for ILLs; in these cases
    // recordInfo.id == mvr.doc_id == -1
    public String getAuthor() {
        String author;
        if (mvr != null) {
            author = mvr.getString("author");
            if (!TextUtils.isEmpty(author)) return author;
        }
        if (acp != null) {
            author = acp.getString("dummy_author");
            if (!TextUtils.isEmpty(author)) return author;
        }
        return "";
    }

    public String getDueDateString() {
        return DateFormat.getDateInstance().format(dueDate);
    }

    public Date getDueDate() {
        return dueDate;
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
