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

import org.evergreen_ils.searchCatalog.CodedValueMap;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.Link;

import java.util.ArrayList;
import java.util.List;

/** AppBehavior - custom app behaviors
 *
 * to override, create a subclass of AppBehavior in the xxx_app module,
 * and specify the name of that class in R.string.ou_behavior_provider.
 */
public class AppBehavior {
    public AppBehavior() {
    }

    private static boolean isOnlineFormat(String icon_format_code) {
        if (TextUtils.isEmpty(icon_format_code))
            return false;
        if (icon_format_code.equals("picture"))
            return true;
        String label = CodedValueMap.iconFormatLabel(icon_format_code);
        return label.startsWith("E-"); // E-book, E-audio
    }

    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;
        return (!TextUtils.isEmpty(record.online_loc)
                && isOnlineFormat(record.getIconFormatLabel()));
    }

    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        ArrayList<Link> links = new ArrayList<>();
        if (TextUtils.isEmpty(record.online_loc))
            return links;
        links.add(new Link(record.online_loc, ""));
        return links;
    }
}
