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

package net.kenstir.apps.pines;

import android.text.TextUtils;

import org.evergreen_ils.android.AppBehavior;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;

public class PinesAppBehavior extends AppBehavior {
    private static final String TAG = PinesAppBehavior.class.getSimpleName();

    public PinesAppBehavior() {
        // loaded through class loader via ou_behavior_provider
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
