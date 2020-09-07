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

import android.content.res.Resources;

import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.data.CopyLocationCounts;
import org.evergreen_ils.data.CopySummary;
import org.evergreen_ils.system.EgCodedValueMap;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.utils.MARCRecord;
import org.evergreen_ils.utils.MARCXMLParser;
import org.evergreen_ils.utils.RecordAttributes;
import org.evergreen_ils.utils.TextUtils;
import org.opensrf.util.GatewayResult;
import org.opensrf.util.OSRFObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.evergreen_ils.utils.StringUtils.safeString;

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

    public boolean hasMetadata = false;
    public boolean hasAttributes = false;
    public boolean hasCopySummary = false;
    public boolean hasMARC = false;

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

        record.isbn = info.getString("isbn");

        try {
            Map<String, ?> subjectMap = (Map<String, ?>) info.get("subject");
            record.subject = TextUtils.join("\n", subjectMap.keySet());
        } catch (Exception e) {
            // ignore
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
            // ignore
        }

        record.hasMetadata = true;
    }

    public void updateFromBREResponse(OSRFObject info) {
        try {
            hasMARC = true;

            String marcxml = info.getString("marc");
            if (!TextUtils.isEmpty(marcxml)) {
                MARCXMLParser parser = new MARCXMLParser(marcxml);
                marc_record = parser.parse();
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
    }

    public void updateFromCopyCountResponse(List<OSRFObject> l) {
        copySummaryList = new ArrayList<>();
        for (OSRFObject obj : l) {
            CopySummary info = new CopySummary(obj);
            copySummaryList.add(info);
        }
        hasCopySummary = true;
    }

    @NonNull
    public String getCopySummary(Resources resources, Integer orgID) {
        int total = 0;
        int available = 0;
        if (copySummaryList == null) return "";
        if (orgID == null) return "";
        for (int i = 0; i < copySummaryList.size(); i++) {
            if (copySummaryList.get(i).getOrgId().equals(orgID)) {
                total = copySummaryList.get(i).getCount();
                available = copySummaryList.get(i).getAvailable();
                break;
            }
        }
        String totalCopies = resources.getQuantityString(R.plurals.number_of_copies, total, total);
        return String.format(resources.getString(R.string.n_of_m_available),
                available, totalCopies, EgOrg.getOrgNameSafe(orgID));
    }

    public void updateFromMRAResponse(OSRFObject mra_obj) {
        attrs = RecordAttributes.parseAttributes(mra_obj);
        hasAttributes = true;
    }

    public @Nullable String getAttr(String attr_name) {
        return attrs.get(attr_name);
    }

//    public String getSearchFormat() {
//        return attrs.get("search_format");
//    }

    public @Nullable String getIconFormat() {
        return (attrs != null) ? attrs.get("icon_format") : null;
    }

    public @NonNull String getIconFormatLabel() {
        return safeString(EgCodedValueMap.iconFormatLabel(getIconFormat()));
    }

    public @NonNull static String getIconFormatLabel(RecordInfo record) {
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

    /** Create array of skeleton records from the multiclass.query response field "ids".
     * The "ids" field is a list of lists and looks like one of:
     *   [[32673,null,"0.0"],[886843,null,"0.0"]]      // integer id,?,?
     *   [["503610",null,"0.0"],["502717",null,"0.0"]] // string id,?,?
     *   [["1805532"],["2385399"]]                     // string id only
     */
    public static ArrayList<RecordInfo> makeArray(List<List<?>> idsList) {
        ArrayList<RecordInfo> records = new ArrayList<>();
        for (int i = 0; i < idsList.size(); i++) {
            Integer record_id = Api.parseInt(idsList.get(i).get(0));
            records.add(new RecordInfo(record_id));
        }
        return records;
    }
}
