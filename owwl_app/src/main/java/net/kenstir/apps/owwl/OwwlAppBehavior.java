/*
 * Copyright (C) 2022 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package net.kenstir.apps.owwl;

import net.kenstir.hemlock.android.AppBehavior;
import org.evergreen_ils.data.MBRecord;
import org.evergreen_ils.utils.Link;

import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class OwwlAppBehavior extends AppBehavior {
    private static final String TAG = OwwlAppBehavior.class.getSimpleName();

    public OwwlAppBehavior() {
        // loaded through class loader via ou_behavior_provider
    }

    @Override @NonNull
    protected String trimLinkTitle(String s) {
        return trimTrailing(s, '.').trim();
    }

    @Override @NonNull
    public List<Link> getOnlineLocations(MBRecord record, String orgShortName) {
        return getOnlineLocationsFromMARC(record, orgShortName);
    }
}
