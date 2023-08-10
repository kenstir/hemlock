/*
 * Copyright (C) 2019 Kenneth H. Cox
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
package net.kenstir.apps.pines

import androidx.annotation.Keep
import org.evergreen_ils.android.AppBehavior
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.utils.Link
import org.evergreen_ils.utils.MARCRecord.MARCDatafield

@Keep
@Suppress("unused")
class PinesAppBehavior : AppBehavior() {
    override fun isVisibleToOrg(df: MARCDatafield, orgShortName: String): Boolean {
        return isVisibleViaLocatedURI(df, orgShortName)
    }

    override fun getOnlineLocations(record: MBRecord, orgShortName: String): List<Link> {
        return getOnlineLocationsFromMARC(record, orgShortName)
    }
}
