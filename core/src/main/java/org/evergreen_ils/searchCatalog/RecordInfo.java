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

import android.text.TextUtils;

import org.evergreen_ils.data.CopyLocationCounts;
import org.evergreen_ils.data.EgCodedValueMap;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.MARCRecord;
import org.evergreen_ils.utils.MARCXMLParser;
import org.evergreen_ils.utils.RecordAttributes;
import org.opensrf.util.GatewayResult;
import org.opensrf.util.OSRFObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.evergreen_ils.system.Utils.safeString;

public class RecordInfo implements Serializable {

    private static final long serialVersionUID = 10123L;

    private static final String TAG = RecordInfo.class.getSimpleName();

    public interface OnRecordClickListener {
        abstract void onClick(RecordInfo record, int position);
    }
    public interface OnRecordLongClickListener {
        abstract void onLongClick(RecordInfo record, int position);
    }

    public Integer doc_id = -1;
    public String title = null;
    public String author = null;
    public String pubdate = null;
    public String publisher = null;
    public String isbn = null;
    public String subject = "";
    public String online_loc = null;
    public String synopsis = null;
    public String physical_description = null;
    public String series = "";

    // todo: put the knowledge of whether this record has been loaded here in RecordInfo
    // and not scattered e.g. in RecordLoader and BasicDetailsFragment
    public boolean basic_metadata_loaded = false;
    public boolean attrs_loaded = false;
    public boolean copy_summary_loaded = false;
    public boolean marcxml_loaded = false;

    public ArrayList<CopySummary> copySummaryList = null;
    public MARCRecord marc_record = null;
    public HashMap<String, String> attrs = null;

    public RecordInfo(int doc_id) {
        this.doc_id = doc_id;
    }

    public RecordInfo(OSRFObject info) {
        updateFromMODSResponse(this, info);
    }

    public static void updateFromMODSResponse(RecordInfo record, Object mods_slim_response) {
        if (mods_slim_response == null)
            return;
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

        record.title = safeString(info.getString("title"));
        record.author = safeString(info.getString("author"));
        record.pubdate = safeString(info.getString("pubdate"));
        record.publisher = safeString(info.getString("publisher"));
        record.synopsis = safeString(info.getString("synopsis"));
        record.physical_description = safeString(info.getString("physical_description"));

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
            List<String> seriesList = (List<String>) info.get("series");
            record.series = TextUtils.join("\n", seriesList);
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        record.basic_metadata_loaded = true;
    }

    public void updateFromBREResponse(GatewayResult response) {
        try {
            OSRFObject info = (OSRFObject) response.payload;
            marcxml_loaded = true;

            String marcxml = info.getString("marc");
            if (!TextUtils.isEmpty(marcxml)) {
                MARCXMLParser parser = new MARCXMLParser(marcxml);
                marc_record = parser.parse();
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
    }

    public static void setCopySummary(RecordInfo record, GatewayResult response) {
        record.copySummaryList = new ArrayList<>();
        if (response == null || response.failed)
            return;
        try {
            List<?> list = (List<?>) response.payload;
            for (Object obj : list) {
                CopySummary info = new CopySummary(obj);
                record.copySummaryList.add(info);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        record.copy_summary_loaded = true;
    }

    public static List<CopyLocationCounts> parseCopyLocationCounts(RecordInfo record, GatewayResult response) {
        List<CopyLocationCounts> copyLocationCountsList = new ArrayList<>();
        if (response == null || response.failed)
            return copyLocationCountsList;
        try {
            return CopyLocationCounts.makeArray(response.asArray());
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        return copyLocationCountsList;
    }

    public void updateFromMRAResponse(GatewayResult response) {
        OSRFObject mra_obj = null;
        try {
            mra_obj = (OSRFObject) response.payload;
        } catch (ClassCastException e) {
            Log.d(TAG, "caught", e);
        }
        updateFromMRAResponse(mra_obj);
    }

    public void updateFromMRAResponse(OSRFObject mra_obj) {
        attrs = RecordAttributes.parseAttributes(mra_obj);
        attrs_loaded = true;
    }

    public String getAttr(String attr_name) {
        return attrs.get(attr_name);
    }

//    public String getSearchFormat() {
//        return attrs.get("search_format");
//    }

    public String getIconFormat() {
        return (attrs != null) ? attrs.get("icon_format") : null;
    }

    public String getIconFormatLabel() {
        return safeString(EgCodedValueMap.iconFormatLabel(getIconFormat()));
    }

    public static String getIconFormatLabel(RecordInfo record) {
        if (record == null)
            return "";
        return record.getIconFormatLabel();
    }

    public String getPublishingInfo() {
        String s = TextUtils.join(" ", new Object[] {
                (pubdate == null) ? "" : pubdate,
                (publisher == null) ? "" : publisher,
        });
        return s.trim();
    }
}
