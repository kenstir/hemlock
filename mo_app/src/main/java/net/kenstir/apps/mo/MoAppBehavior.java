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

package net.kenstir.apps.mo;

import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;
import org.evergreen_ils.data.MBRecord;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class MoAppBehavior extends AppBehavior {
    private static final String TAG = MoAppBehavior.class.getSimpleName();

    public MoAppBehavior() {
        // loaded through class loader via ou_behavior_provider
    }

    @Override
    public Boolean isOnlineResource(MBRecord record) {
        if (record == null) return null;
        if (!record.hasMetadata()) return null;
        if (!record.hasAttributes()) return null;

        // TODO: verify if correct
        String item_form = record.getAttr("item_form");
        if (TextUtils.equals(item_form, "o")
                || TextUtils.equals(item_form, "s"))
            return true;

        return false;
    }

    // Trim the link text for a better mobile UX
    protected String trimLinkTitle(String s) {
        return s.replaceAll("click here", "").trim();
    }

    @Override
    protected boolean isVisibleToOrg(MARCRecord.MARCDatafield df, String orgShortName) {
        // Don't filter URIs because the query already did.  For a good UX we show all URIs
        // located by the search and let the link text and the link itself control access.
        // See also Located URIs in docs/cataloging/cataloging_electronic_resources.adoc
        return true;
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(MBRecord record, String orgShortName) {
        return getOnlineLocationsFromMARC(record, orgShortName);
    }
}
