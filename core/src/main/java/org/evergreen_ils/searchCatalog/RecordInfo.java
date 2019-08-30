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

import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.utils.MARCRecord;
import org.evergreen_ils.utils.MARCXMLParser;
import org.evergreen_ils.utils.RecordAttributes;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public String doc_type = null;
    public String online_loc = null;
    public String synopsis = null;
    public String physical_description = null;
    public String series = "";
    public String item_form = null;
    public boolean dummy = false;

    // todo: put the knowledge of whether this record has been loaded here in RecordInfo
    // and not scattered e.g. in RecordLoader and BasicDetailsFragment
    public boolean basic_metadata_loaded = false;
    public boolean attrs_loaded = false;
    public boolean copy_summary_loaded = false;
    public boolean marcxml_loaded = false;

    public ArrayList<CopySummary> copySummaryList = null;
    public String search_format = null;
    public MARCRecord marc_record = null;
    public HashMap<String, String> attrs = null;

    public RecordInfo() {
        this.dummy = true;
    }

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

        record.title = Utils.safeString(info.getString("title"));
        record.author = Utils.safeString(info.getString("author"));
        record.pubdate = Utils.safeString(info.getString("pubdate"));
        record.publisher = Utils.safeString(info.getString("publisher"));
        record.doc_type = Utils.safeString(info.getString("doc_type"));
        record.synopsis = Utils.safeString(info.getString("synopsis"));
        record.item_form = Utils.safeString(info.getString("item_form"));

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

    public void updateFromBREResponse(GatewayResponse response) {
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

    public static void setCopySummary(RecordInfo record, GatewayResponse response) {
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

    public static List<CopyLocationCounts> parseCopyLocationCounts(RecordInfo record, GatewayResponse response) {
        List<CopyLocationCounts> copyLocationCountsList = new ArrayList<>();
        if (response == null || response.failed)
            return copyLocationCountsList;
        try {
            List<List<Object>> list = (List<List<Object>>) response.payload;
            for (List<Object> elem : list) {
                CopyLocationCounts copyInfo = new CopyLocationCounts(elem);
                copyLocationCountsList.add(copyInfo);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        return copyLocationCountsList;
    }

    public void setSearchFormat(String format) {
        search_format = format;
    }

    public void updateFromMRAResponse(GatewayResponse response) {
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

        // the "icon_format" attr (e.g. ebook) is what we need, not
        // the "search_format" attr (e.g. "electronic")
        search_format = attrs.get("icon_format");
        if (TextUtils.isEmpty(search_format))
            search_format = attrs.get("search_format");
        attrs_loaded = true;
    }

    public String getAttr(String attr_name) {
        return attrs.get(attr_name);
    }

    public static String getFormatLabel(RecordInfo record) {
        if (record == null)
            return "";
        return SearchFormat.getItemLabelFromSearchFormat(record.search_format);
    }

    public String getPublishingInfo() {
        String s = TextUtils.join(" ", new Object[] {
                (pubdate == null) ? "" : pubdate,
                (publisher == null) ? "" : publisher,
        });
        return s.trim();
    }
}
