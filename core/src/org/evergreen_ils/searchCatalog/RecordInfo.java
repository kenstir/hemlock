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
package org.evergreen_ils.searchCatalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.text.TextUtils;
import android.util.Log;
import org.opensrf.util.OSRFObject;

public class RecordInfo implements Serializable {

    private static final long serialVersionUID = 10123L;

    private static final String TAG = RecordInfo.class.getSimpleName();

    public String title = null;
    public String author = null;
    public String pubdate = null;
    public String isbn = null;
    public Integer doc_id = null;
    public String publisher = null;
    public String subject = "";
    public String doc_type = null;
    public String online_loc = null;
    public String synopsis = null;
    public String physical_description = null;
    public String series = "";
    public boolean dummy = false;

    public ArrayList<CopyCountInformation> copyCountListInfo = null;

    public List<CopyInformation> copyInformationList = null;
    public String search_format = null;

    public RecordInfo() {
        this.title = "Test title";
        this.author = "Test author";
        this.pubdate = "Publication date";
        copyInformationList = new ArrayList<CopyInformation>();

        // marks the fact that this is a record made from no info
        this.dummy = true;
    }

    private String safeGetString(OSRFObject info, String field) {
        String s = info.getString(field);
        if (s == null)
            return "";
        return s;
    }

    private String safeString(String s) {
        if (s == null)
            return "";
        return s;
    }

    public RecordInfo(OSRFObject info) {
        copyInformationList = new ArrayList<CopyInformation>();
        try {
            this.title = safeString(info.getString("title"));
            this.author = safeString(info.getString("author"));
            this.pubdate = safeString(info.getString("pubdate"));
            this.publisher = safeString(info.getString("publisher"));
            this.doc_id = info.getInt("doc_id");
            this.doc_type = safeString(info.getString("doc_type"));
        } catch (Exception e) {
            Log.d(TAG, "Exception retrieving basic info", e);
        }

        try {
            this.isbn = (String) info.get("isbn");
        } catch (Exception e) {
            Log.d(TAG, "Exception retrieving isbn", e);
        }

        try {
            Map<String, ?> subjectMap = (Map<String, ?>) info.get("subject");
            this.subject = TextUtils.join("\n", subjectMap.keySet());
        } catch (Exception e) {
            Log.d(TAG, "Exception retrieving subject", e);
        }

        try {
            this.online_loc = ((List) info.get("online_loc")).get(0).toString();
        } catch (Exception e) {
            // common
        }

        try {
            this.physical_description = (String) info.get("physical_description");
        } catch (Exception e) {
            Log.d(TAG, "Exception retrieving physical_description", e);
        }

        try {
            List<String> seriesList = (List<String>) info.get("series");
            this.series = TextUtils.join(", ", seriesList);
        } catch (Exception e) {
            Log.d(TAG, "Exception retrieving series", e);
        }
    }
}
