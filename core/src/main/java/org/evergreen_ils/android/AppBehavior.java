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

package org.evergreen_ils.android;

import android.text.TextUtils;

import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.data.MBRecord;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** AppBehavior - custom app behaviors
 *
 * to override, create a subclass of AppBehavior in the xxx_app module,
 * and specify the name of that class in R.string.ou_behavior_provider.
 */
public class AppBehavior {
    public AppBehavior() {
    }

    protected String trimTrailing(String s, char c) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == c) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private boolean isOnlineFormat(String icon_format_label) {
        if (TextUtils.isEmpty(icon_format_label))
            return false;
        if (icon_format_label.equals("Picture"))
            return true;
        return (icon_format_label.startsWith("E-")); // E-book, E-audio
    }

    @Nullable
    public Boolean isOnlineResource(MBRecord record) {
        if (record == null) return null;
        if (!record.hasMetadata) return null;
        if (!record.hasAttributes) return null;

        String item_form = record.getAttr("item_form");
        if (TextUtils.equals(item_form, "o")
                || TextUtils.equals(item_form, "s"))
            return true;

        return isOnlineFormat(record.getIconFormatLabel());
    }

    // Trim the link text for a better mobile UX
    protected String trimLinkTitle(String s) {
        return s;
    }

    // Is this MARC datafield a URI visible to this org?
    protected boolean isVisibleToOrg(MARCRecord.MARCDatafield df, String orgShortName) {
        return true;
    }

    // Implements the above interface for catalogs that use Located URIs
    protected boolean isVisibleViaLocatedURI(MARCRecord.MARCDatafield df, String orgShortName) {
        List<String> ancestors = EgOrg.getOrgAncestry(orgShortName);
        for (MARCRecord.MARCSubfield sf : df.subfields) {
            if (TextUtils.equals(sf.code, "9") && ancestors.contains(sf.text)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    protected List<Link> getOnlineLocationsFromMARC(MBRecord record, String orgShortName) {
        ArrayList<Link> links = new ArrayList<>();
        if (!record.hasMARC || record.marc_record == null)
            return links;

        for (MARCRecord.MARCDatafield df: record.marc_record.datafields) {
            if (df.isOnlineLocation()
                    && isVisibleToOrg(df, orgShortName))
            {
                String href = df.getUri();
                String text = df.getLinkText();
                if (href != null && text != null) {
                    Link link = new Link(href, trimLinkTitle(text));
                    // Filter duplicate links
                    if (!links.contains(link)) {
                        links.add(link);
                    }
                }
            }
        }

        Collections.sort(links, new Comparator<Link>() {
            @Override
            public int compare(Link a, Link b) {
                return a.getText().compareTo(b.getText());
            }
        });

        return links;
    }

    @NonNull
    public List<Link> getOnlineLocations(MBRecord record, String orgShortName) {
        ArrayList<Link> links = new ArrayList<>();
        if (TextUtils.isEmpty(record.online_loc))
            return links;
        links.add(new Link(record.online_loc, ""));
        return links;
    }
}
