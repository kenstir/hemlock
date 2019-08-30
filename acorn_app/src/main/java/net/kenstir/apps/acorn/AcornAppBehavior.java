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

package net.kenstir.apps.acorn;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;

import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.Link;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class AcornAppBehavior extends AppBehavior {
    public AcornAppBehavior() {
        // NB: this looks unused but it is used
    }

    @Override
    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;
//        if (!record.marcxml_loaded) return null;
        return TextUtils.equals(record.getAttr("item_form"), "o");
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        if (!record.marcxml_loaded || record.marc_record == null)
            return new ArrayList<>();
        return record.marc_record.getLinks();
    }
}
