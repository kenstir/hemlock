/*
 * Copyright (c) 2023 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package net.kenstir.apps.noble

import android.text.TextUtils
import androidx.annotation.Keep
import net.kenstir.hemlock.android.AppBehavior
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.utils.Link
import org.evergreen_ils.utils.MARCRecord.MARCDatafield

@Keep
class NobleAppBehavior : AppBehavior() {
    override fun isOnlineResource(record: MBRecord?): Boolean? {
        if (record == null) return null
        if (!record.hasMetadata()) return null
        if (!record.hasAttributes()) return null

        val item_form = record.getAttr("item_form")
        if (TextUtils.equals(item_form, "o")
            || TextUtils.equals(item_form, "s")) {
            return true
        }

        return false
    }

    // Trim the link text for a better mobile UX
    override fun trimLinkTitle(s: String): String {
        return s
    }

    override fun isVisibleToOrg(df: MARCDatafield, orgShortName: String): Boolean {
        // Don't filter URIs because the query already did.  For a good UX we show all URIs
        // located by the search and let the link text and the link itself control access.
        // See also Located URIs in docs/cataloging/cataloging_electronic_resources.adoc
        return true
    }

    override fun getOnlineLocations(record: MBRecord, orgShortName: String): List<Link> {
        return getOnlineLocationsFromMARC(record, orgShortName)
    }

    companion object {
        private val TAG = NobleAppBehavior::class.java.simpleName
    }
}