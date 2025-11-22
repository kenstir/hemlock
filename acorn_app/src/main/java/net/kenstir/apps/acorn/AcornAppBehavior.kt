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
package net.kenstir.apps.acorn

import androidx.annotation.Keep
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.Link
import net.kenstir.logging.Log
import net.kenstir.ui.AppBehavior
import org.evergreen_ils.data.model.MARCRecord.MARCDatafield
import org.evergreen_ils.data.model.MBRecord

@Keep
@Suppress("unused")
class AcornAppBehavior : AppBehavior() {
    private fun isOnlineFormatCode(icon_format_code: String?): Boolean {
        val onlineFormatCodes = listOf("ebook", "eaudio", "evideo", "emusic")
        return onlineFormatCodes.contains(icon_format_code)
    }

    override fun isOnlineResource(record: MBRecord?): Boolean? {
        if (record == null) return null
        if (!record.hasMetadata()) return null
        if (!record.hasAttributes()) return null

        val iconFormatCode = record.iconFormat
        if (record.getAttr("item_form") == "o") {
            Log.d("isOnlineResource", "title:" + record.title + " item_form:o icon_format:" + iconFormatCode + "-> true")
            return true
        }

        // NB: Checking for item_form="o" fails to identify some online resources, e.g.
        // https://acorn.biblio.org/eg/opac/record/2891957
        // (search Bethel Public Library for "potter prisoner" ebook)
        // so we use this check as a backstop
        return isOnlineFormatCode(record.iconFormat)
    }

    override fun trimLinkTitle(s: String): String {
        val s1 = s.replace("Click here to (download|access)\\.?".toRegex(), "")
            .trim()
        return trimTrailing(s1, '.').trim()
    }

    override fun isVisibleToOrg(df: MARCDatafield, orgShortName: String): Boolean {
        return isVisibleViaLocatedURI(df, orgShortName)
    }

    override fun getOnlineLocations(record: BibRecord, orgShortName: String): List<Link> {
        return getOnlineLocationsFromMARC(record, orgShortName)
    }
}
