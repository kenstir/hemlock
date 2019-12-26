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

import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;

import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
public class AcornAppBehavior extends AppBehavior {
    private static final String TAG = AcornAppBehavior.class.getSimpleName();

    public AcornAppBehavior() {
        // loaded through class loader via ou_behavior_provider
    }

    private boolean isOnlineFormatCode(String icon_format_code) {
        String[] onlineFormatCodes = {"ebook","eaudio","evideo","emusic"};
        return Arrays.asList(onlineFormatCodes).contains(icon_format_code);
    }

    @Override
    public Boolean isOnlineResource(RecordInfo record) {
        if (!record.basic_metadata_loaded) return null;
        if (!record.attrs_loaded) return null;

        String iconFormatCode = record.getIconFormat();
        if (TextUtils.equals(record.getAttr("item_form"), "o")) {
            Log.d("isOnlineResource", "title:" + record.title + " item_form:o icon_format:" + iconFormatCode + "-> true");
            return true;
        }

        // NB: Checking for item_form="o" fails to identify some online resources, e.g.
        // https://acorn.biblio.org/eg/opac/record/2891957
        // so we use this check as a backstop
        return isOnlineFormatCode(record.getIconFormat());
    }

    @Override
    protected String trimLinkTitle(String s) {
        String s1 = s.replaceAll("Click here to (download|access)\\.?", "")
                .trim();
        return trimTrailing(s1,'.').trim();
    }

    @Override
    protected boolean isVisibleToOrg(MARCRecord.MARCDatafield df, String orgShortName) {
        return isVisibleViaLocatedURI(df, orgShortName);
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(RecordInfo record, String orgShortName) {
        return getOnlineLocationsFromMARC(record, orgShortName);
    }
}
