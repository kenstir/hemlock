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

import java.util.Date;

import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.opensrf.util.OSRFObject;

/**
 * This is a wrapper class that get the information out for a circ object
 * 
 * @author daniel
 * 
 */
public class CircRecord {

    public static final int MVR_OBJ_TYPE = 1;
    public static final int ACP_OBJ_TYPE = 2;
    public static final int UNDEF_OBJ_TYPE = 0;
    public enum CircType { OUT, OVERDUE, LONG_OVERDUE, LOST, CLAIMS_RETURNED };

    public OSRFObject mvr = null;
    public OSRFObject acp = null;
    public OSRFObject circ = null;
    public RecordInfo recordInfo = null;

    public int circ_info_type = UNDEF_OBJ_TYPE;

    public CircType circ_type;

    public int circ_id = -1;

    private Date circ_due_date = null;

    public CircRecord(OSRFObject circ, OSRFObject mvr, OSRFObject acp, CircType circ_type, int circ_id) {

        this.circ = circ;

        // one of the acp or mvr will be null this will determine the circ
        // OSRFObject type
        this.acp = acp;
        this.mvr = mvr;

        if (this.acp != null)
            this.circ_info_type = ACP_OBJ_TYPE;

        if (this.mvr != null)
            this.circ_info_type = MVR_OBJ_TYPE;

        this.circ_type = circ_type;
        this.circ_id = circ_id;
        this.circ_due_date = GlobalConfigs.parseDate(circ.getString("due_date"));
    }

    public CircRecord(OSRFObject circ, CircType circ_type, int circ_id) {
        this.circ = circ;
        this.circ_type = circ_type;
        this.circ_id = circ_id;
        this.circ_due_date = GlobalConfigs.parseDate(circ.getString("due_date"));
    }

    public String getAuthor() {

        String author = null;

        if (this.circ_info_type == MVR_OBJ_TYPE)
            author = mvr.getString("author");
        if (this.circ_info_type == ACP_OBJ_TYPE)
            author = acp.getString("dummy_author");

        return author;
    }

    public String getDueDate() {

        return circ_due_date.toLocaleString();
    }

    public Date getDueDateObject() {
        return circ_due_date;
    }

    public String getTitle() {

        String title = null;

        if (this.circ_info_type == MVR_OBJ_TYPE)
            title = mvr.getString("title");
        if (this.circ_info_type == ACP_OBJ_TYPE)
            title = acp.getString("dummy_title");

        return title;
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
        Date currentDate = new Date(System.currentTimeMillis());
        return getDueDateObject().compareTo(currentDate) < 0;
    }

    public String getFormatLabel() {
        if (recordInfo == null)
            return "";
        return SearchFormat.getItemLabelFromSearchFormat(recordInfo.search_format);
    }
}
