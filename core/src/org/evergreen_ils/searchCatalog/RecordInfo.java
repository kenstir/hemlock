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

import android.text.TextUtils;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.globals.Utils;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;

public class RecordInfo implements Serializable {

    private static final long serialVersionUID = 10123L;

    private static final String TAG = RecordInfo.class.getSimpleName();

    public Integer doc_id = -1;
    public String title = null;
    public String author = null;
    public String pubdate = null;
    public String publisher = null;
    public String isbn = null;
    public String subject = "";
    public String doc_type = null;
    public String online_loc = null;
    public String synopsis = null;
    public String physical_description = null;
    public String series = "";
    public boolean dummy = false;

    // todo: put the knowledge of whether this record has been loaded here in RecordInfo
    // and not scattered e.g. in RecordLoader and BasicDetailsFragment
    public boolean basic_metadata_loaded = false;
    public boolean search_format_loaded = false;
    public boolean copy_info_loaded = false;

    public ArrayList<CopyCountInformation> copyCountInformationList = null;
    public List<CopyInformation> copyInformationList = null;
    public String search_format = null;

    public RecordInfo() {
        // marks the fact that this is a record made from no info
        this.dummy = true;
    }

    public RecordInfo(int doc_id) {
        this.doc_id = doc_id;
    }

    public RecordInfo(OSRFObject info) {
        updateFromMODSResponse(this, info);
    }

    public static void updateFromMODSResponse(RecordInfo record, Object mods_slim_response) {
        OSRFObject info;
        try {
            info = (OSRFObject) mods_slim_response;
        } catch (ClassCastException e) {
            Log.d(TAG, "caught", e);
            return;
        }

        try {
            if (record.doc_id == -1)
                record.doc_id = info.getInt("doc_id");
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        record.title = Utils.safeString(info.getString("title"));
        record.author = Utils.safeString(info.getString("author"));
        record.pubdate = Utils.safeString(info.getString("pubdate"));
        record.publisher = Utils.safeString(info.getString("publisher"));
        record.doc_type = Utils.safeString(info.getString("doc_type"));
        record.synopsis = Utils.safeString(info.getString("synopsis"));

        try {
            record.isbn = (String) info.get("isbn");
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        try {
            Map<String, ?> subjectMap = (Map<String, ?>) info.get("subject");
            record.subject = TextUtils.join("\n", subjectMap.keySet());
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        try {
            record.online_loc = ((List) info.get("online_loc")).get(0).toString();
        } catch (Exception e) {
            // common
        }

        try {
            record.physical_description = (String) info.get("physical_description");
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        try {
            List<String> seriesList = (List<String>) info.get("series");
            record.series = TextUtils.join("\n", seriesList);
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        record.basic_metadata_loaded = true;
    }

    public static void setCopyCountInfo(RecordInfo record, GatewayResponse response) {
        record.copyCountInformationList = new ArrayList<CopyCountInformation>();
        if (response == null || response.failed)
            return;
        try {
            List<?> list = (List<?>) response.payload;
            for (Object obj : list) {
                CopyCountInformation info = new CopyCountInformation(obj);
                record.copyCountInformationList.add(info);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        record.copy_info_loaded = true;
    }

    public void setSearchFormat(String format) {
        search_format = format;
        search_format_loaded = true;
    }

    public static String getFormatLabel(RecordInfo record) {
        if (record == null)
            return "";
        return SearchFormat.getItemLabelFromSearchFormat(record.search_format);
    }

    public String getPublishingInfo() {
        String s = TextUtils.join(" ", new Object[] {pubdate, publisher});
        return s.trim();
    }

    /** some records have a non-null online_loc but are books nevertheless, e.g.
     * https://bark.cwmars.org/eg/opac/record/3306629
     */
    public boolean isOnlineResource() {
        return (!TextUtils.isEmpty(online_loc)
                && SearchFormat.isOnlineResource(search_format));
    }
}
