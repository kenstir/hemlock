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
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class NobleAppBehavior extends AppBehavior {
    private static final String TAG = NobleAppBehavior.class.getSimpleName();

    public NobleAppBehavior() {
        // loaded through class loader via ou_behavior_provider
    }

    @Override
    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;

        String item_form = record.getAttr("item_form");
        if (TextUtils.equals(item_form, "o")
                || TextUtils.equals(item_form, "s"))
            return true;

        return false;
    }

    // Trim the link text for a better mobile UX
    protected String trimLinkTitle(String s) {
        return s;
    }

    @Override
    protected boolean isVisibleToOrg(MARCRecord.MARCDatafield df, String orgShortName) {
        // Don't filter URIs because the query already did.  For a good UX we show all URIs
        // located by the search and let the link text and the link itself control access.
        // See also Located URIs in docs/cataloging/cataloging_electronic_resources.adoc
        return true;
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        return getOnlineLocationsFromMARC(record, orgShortName);
    }
}
