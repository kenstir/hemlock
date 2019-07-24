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

import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.SearchFormat;
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

    private static boolean isOnlineFormat(String search_format) {
        if (TextUtils.isEmpty(search_format))
            return false;
        if (search_format.equals("picture"))
            return true;
        String label = SearchFormat.getItemLabelFromSearchFormat(search_format);
        return label.startsWith("E-"); // E-book, E-audio
    }

    public boolean isOnlineResource(RecordInfo record) {
        return (!TextUtils.isEmpty(record.online_loc)
                && isOnlineFormat(record.search_format));
    }

    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        ArrayList<Link> links = new ArrayList<>();
        if (TextUtils.isEmpty(record.online_loc))
            return links;
        links.add(new Link(record.online_loc, ""));
        return links;
    }
}
