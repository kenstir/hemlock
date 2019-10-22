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

import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;

public class NobleAppBehavior extends AppBehavior {
    private static final String TAG = NobleAppBehavior.class.getSimpleName();

    public NobleAppBehavior() {
        // NB: this looks unused but it is used
    }

    @Override
    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;

        String item_form = record.getAttr("item_form");
        Log.d("kcxxx.os", "id:"+record.doc_id+" title:"+record.title+" item_form:"+item_form+" icon_format:"+record.getIconFormat());
        if (TextUtils.equals(item_form, "o")
                || TextUtils.equals(item_form, "s"))
            return true;

        return false;
    }

    // Trim the link text for a better mobile UX
    private String trimLinkTitle(String s) {
        String s1 = s.replaceAll("Click here to (download|access)\\.?", "")
                .trim();
        return trimTrailing(s1,'.').trim();
    }

    // TODO: check all org levels between orgShortName and consortium.  In practice, it seems
    // electronic items are available to either the branch or the consortium, so this is Good Enough.
    // See also Located URIs in docs/cataloging/cataloging_electronic_resources.adoc
    private boolean isAvailableToOrg(MARCRecord.MARCDatafield df, String orgShortName, String consortiumShortName) {
        for (MARCRecord.MARCSubfield sf : df.subfields) {
            if (TextUtils.equals(sf.code, "9")
                    && (TextUtils.equals(sf.text, orgShortName)
                        || TextUtils.equals(sf.text, consortiumShortName)
                        || orgShortName == null))
            {
                return true;
            }
        }
        return false;
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        ArrayList<Link> links = new ArrayList<>();
        if (!record.marcxml_loaded || record.marc_record == null)
            return links;

        Organization consortium = EvergreenServer.getInstance().getOrganization(Organization.consortiumOrgId);

        // Eliminate duplicates by href
        HashSet<String> seen = new HashSet<>();

        for (MARCRecord.MARCDatafield df: record.marc_record.datafields) {
            if (TextUtils.equals(df.tag, "856")
                    && TextUtils.equals(df.ind1, "4")
                    && (TextUtils.equals(df.ind2, "0") || TextUtils.equals(df.ind2, "1"))
                    && isAvailableToOrg(df, orgShortName, consortium.shortname))
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
