/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.evergreen_ils.data;

import android.content.res.Resources;

import org.evergreen_ils.OSRFUtils;
import org.evergreen_ils.R;
import org.evergreen_ils.system.EgCodedValueMap;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.utils.MARCRecord;
import org.evergreen_ils.utils.MARCXMLParser;
import org.evergreen_ils.utils.RecordAttributes;
import org.evergreen_ils.utils.TextUtils;
import org.opensrf.util.OSRFObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.evergreen_ils.utils.StringUtils.safeString;

public class MBRecord implements Serializable {

    private static final long serialVersionUID = 10123L;

    private static final String TAG = MBRecord.class.getSimpleName();

    public Integer id = -1;
    public String title = "";
    public String author = "";
    public String pubdate = null;
    public String publisher = null;
    public String isbn = null;
    public String subject = "";
    public String online_loc = null;
    public String synopsis = null;
    public String physicalDescription = null;
    public String series = "";

    public boolean hasMetadata = false;
    public boolean isDeleted = false;

    public ArrayList<CopyCount> copyCounts = null;
    public MARCRecord marcRecord = null;
    public HashMap<String, String> attrs = null;

    public MBRecord(int id) {
        this.id = id;
    }

    public MBRecord(OSRFObject info) {
        updateFromMODSResponse(info);
    }

    public void updateFromMODSResponse(@NonNull OSRFObject info) {
        try {
            if (id == -1)
                id = info.getInt("doc_id");
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        title = safeString(info.getString("title"));
        author = safeString(info.getString("author"));
        pubdate = safeString(info.getString("pubdate"));
        publisher = safeString(info.getString("publisher"));
        synopsis = safeString(info.getString("synopsis"));
        physicalDescription = safeString(info.getString("physical_description"));

        isbn = info.getString("isbn");

        try {
            Map<String, ?> subjectMap = (Map<String, ?>) info.get("subject");
            subject = TextUtils.join("\n", subjectMap.keySet());
        } catch (Exception e) {
            // ignore
        }

        try {
            online_loc = ((List) info.get("online_loc")).get(0).toString();
        } catch (Exception e) {
            // common
        }

        try {
            List<String> seriesList = (List<String>) info.get("series");
            series = TextUtils.join("\n", seriesList);
        } catch (Exception e) {
            // ignore
        }

        hasMetadata = true;
    }

    public void updateFromBREResponse(OSRFObject breObj) {
        try {
            isDeleted = breObj.getBoolean("deleted");
            Log.d(TAG, "[kcxxx] record ${doc_id}: deleted=${isDeleted}");

            String marcxml = breObj.getString("marc");
            if (!TextUtils.isEmpty(marcxml)) {
                MARCXMLParser parser = new MARCXMLParser(marcxml);
                marcRecord = parser.parse();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @NonNull
    public String getCopySummary(Resources resources, Integer orgID) {
        int total = 0;
        int available = 0;
        if (copyCounts == null) return "";
        if (orgID == null) return "";
        for (int i = 0; i < copyCounts.size(); i++) {
            if (copyCounts.get(i).getOrgId().equals(orgID)) {
                total = copyCounts.get(i).getCount();
                available = copyCounts.get(i).getAvailable();
                break;
            }
        }
        String totalCopies = resources.getQuantityString(R.plurals.number_of_copies, total, total);
        return String.format(resources.getString(R.string.n_of_m_available),
                available, totalCopies, EgOrg.getOrgNameSafe(orgID));
    }

    public void updateFromMRAResponse(OSRFObject mraObj) {
        attrs = RecordAttributes.parseAttributes(mraObj);
    }

    public @Nullable String getAttr(String attr_name) {
        return attrs.get(attr_name);
    }

    public @Nullable String getIconFormat() {
        return (attrs != null) ? attrs.get("icon_format") : null;
    }

    public @NonNull String getIconFormatLabel() {
        return safeString(EgCodedValueMap.iconFormatLabel(getIconFormat()));
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
    public static ArrayList<MBRecord> makeArray(List<List<?>> idsList) {
        ArrayList<MBRecord> records = new ArrayList<>();
        for (int i = 0; i < idsList.size(); i++) {
            Integer id = OSRFUtils.parseInt(idsList.get(i).get(0));
            records.add(new MBRecord(id));
        }
        return records;
    }
}
