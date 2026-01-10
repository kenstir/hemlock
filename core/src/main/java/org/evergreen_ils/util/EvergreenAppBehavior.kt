/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package org.evergreen_ils.util

import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.Link
import net.kenstir.data.model.MARCRecord
import net.kenstir.logging.Log
import net.kenstir.ui.AppBehavior
import org.evergreen_ils.system.EgOrg

open class EvergreenAppBehavior : AppBehavior() {
    private fun isOnlineFormat(iconFormatLabel: String?): Boolean {
        if (iconFormatLabel.isNullOrEmpty()) return false
        if (iconFormatLabel == "Picture") return true
        return (iconFormatLabel.startsWith("E-")) // E-book, E-audio
    }

    override fun isOnlineResource(record: BibRecord?): Boolean? {
        if (record == null) return null
        if (!record.hasMetadata()) return null
        if (!record.hasAttributes()) return null

        val itemForm = record.getAttr("item_form")
        if (itemForm == "o"
            || itemForm == "s") return true

        return isOnlineFormat(record.iconFormatLabel)
    }

    /** Trims the link text for a better mobile UX  */
    protected open fun trimLinkTitle(s: String): String {
        return s
    }

    /** Returns true if this MARC datafield is a URI visible to the specifice org
     *
     * The default implementation always returns true; override this method
     * to implement Evergreen's Located URIs or other access control schemes.
     *
     * See also [isVisibleViaLocatedURI].
     */
    protected open fun isVisibleToOrg(df: MARCRecord.MARCDatafield, orgShortName: String): Boolean {
        return true
    }

    /** Implements [isVisibleToOrg] for catalogs that use Evergreen's Located URIs */
    protected fun isVisibleViaLocatedURI(df: MARCRecord.MARCDatafield, orgShortName: String): Boolean {
        val subfield9s: MutableList<String?> = ArrayList<String?>()
        for (sf in df.subfields) {
            if (sf.code == "9") {
                subfield9s.add(sf.text)
            }
        }

        // the item is visible if there are no subfield 9s limiting access
        if (subfield9s.isEmpty()) {
            return true
        }

        // otherwise it is visible if subfield 9 is this org or an ancestor of it
        val ancestors = EgOrg.getOrgAncestry(orgShortName)
        for (s in subfield9s) {
            if (ancestors.contains(s)) {
                return true
            }
        }
        return false
    }

    fun getOnlineLocationsFromMARC(record: BibRecord, orgShortName: String): List<Link> {
        val marcRecord = record.marcRecord ?: return ArrayList()

        return getLinksFromMARCRecord(marcRecord, orgShortName)
    }

    fun getLinksFromMARCRecord(marcRecord: MARCRecord, orgShortName: String): List<Link> {
        val links = ArrayList<Link>()
        for (df in marcRecord.datafields) {
            Log.d("marc", "tag=" + df.tag + " ind1=" + df.ind1 + " ind2=" + df.ind2)
            if (df.isOnlineLocation
                && isVisibleToOrg(df, orgShortName)) {
                val href = df.uri
                val text = df.linkText
                if (href != null && text != null) {
                    val link = Link(href, trimLinkTitle(text))
                    // Filter duplicate links
                    if (!links.contains(link)) {
                        links.add(link)
                    }
                }
            }
        }

        // I don't know where I got the notion to sort these;
        // I don't see that done in the OPAC.
//        Collections.sort(links, new Comparator<Link>() {
//            @Override
//            public int compare(Link a, Link b) {
//                return a.getText().compareTo(b.getText());
//            }
//        });
        return links
    }

    override fun getOnlineLocations(record: BibRecord, orgShortName: String): List<Link> {
        val links = ArrayList<Link>()
        val onlineLoc = record.getFirstOnlineLocation()
        if (onlineLoc.isNullOrEmpty()) {
            return links
        }
        links.add(Link(onlineLoc, ""))
        return links
    }
}
