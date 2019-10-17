//
//  Copyright (C) 2019 Kenneth H. Cox
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package net.kenstir.apps.noble;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;

import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NobleAppBehavior extends AppBehavior {
    private static final String TAG = NobleAppBehavior.class.getSimpleName();

    public NobleAppBehavior() {
        // NB: this looks unused but it is used
    }

    @Override
    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;

        // NB: Checking for item_form="o" fails to identify some online resources, e.g.
        // https://acorn.biblio.org/eg/opac/record/2891957
        // However, it's better than waiting for the marcxml to load, if the network is slow
        if (TextUtils.equals(record.getAttr("item_form"), "o"))
            return true;

        if (!record.marcxml_loaded) return null;
        return (getOnlineLocations(record, null).size() > 0);
    }

    private String trimTrailing(String s, char c) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == c) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    // Trim the link text for a better mobile UX
    private String trimLinkTitle(String s) {
        String s1 = s.replaceAll("Click here to (download|access)\\.?", "")
                .trim();
        return trimTrailing(s1,'.').trim();
    }

    private boolean isAvailableToOrg(MARCRecord.MARCDatafield df, String orgShortName) {
        for (MARCRecord.MARCSubfield sf : df.subfields) {
            if (TextUtils.equals(sf.code, "9") && (TextUtils.equals(sf.text, orgShortName) || orgShortName == null)) {
                return true;
            }
        }
        return false;
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        if (!record.marcxml_loaded || record.marc_record == null)
            return new ArrayList<>();

        // Include only links that are available to this org
        ArrayList<Link> links = new ArrayList<>();

        // Eliminate duplicates by href
        HashSet<String> seen = new HashSet<>();

        for (MARCRecord.MARCDatafield df: record.marc_record.datafields) {
            if (TextUtils.equals(df.tag, "856")
                    && TextUtils.equals(df.ind1, "4")
                    && (TextUtils.equals(df.ind2, "0") || TextUtils.equals(df.ind2, "1"))
                    && isAvailableToOrg(df, orgShortName))
            {
                String href = null;
                String text = null;
                for (MARCRecord.MARCSubfield sf: df.subfields) {
                    if (TextUtils.equals(sf.code, "u") && href == null) href = sf.text;
                    if ((TextUtils.equals(sf.code, "3") || TextUtils.equals(sf.code, "y")) && text == null) text = sf.text;
                }
                if (href != null && text != null && !seen.contains(href)) {
                    links.add(new Link(href, trimLinkTitle(text)));
                    seen.add(href);
                }
            }
        }
        return links;
    }
}
