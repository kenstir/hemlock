/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */
package net.kenstir.apps.mo

import androidx.annotation.Keep
import net.kenstir.data.model.Link
import net.kenstir.ui.AppBehavior
import org.evergreen_ils.data.model.MARCRecord.MARCDatafield
import org.evergreen_ils.data.model.MBRecord

@Keep
@Suppress("unused")
class MoAppBehavior : AppBehavior() {
    override fun isOnlineResource(record: MBRecord?): Boolean? {
        if (record == null) return null
        if (!record.hasMetadata()) return null
        if (!record.hasAttributes()) return null

        // TODO: verify if correct
        val itemForm = record.getAttr("item_form")
        if (itemForm == "o" || itemForm == "s") {
            return true
        }

        return false
    }

    override fun trimLinkTitle(s: String): String {
        return s.replace("click here".toRegex(), "").trim()
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
}
