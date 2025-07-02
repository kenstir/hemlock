/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 *
 */
package org.evergreen_ils.system

import net.kenstir.hemlock.data.model.BibRecord
import net.kenstir.hemlock.data.model.Organization
import net.kenstir.hemlock.net.SearchResults
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.data.OSRFUtils
import org.evergreen_ils.xdata.XOSRFObject
import java.util.ArrayList

object EgSearch {
    var selectedOrganization: Organization? = null
    var visible = 0
    var searchLimit = 100

    val lastResults: SearchResults
        get() = EvergreenSearchResults()

    private val records: ArrayList<MBRecord> = ArrayList(searchLimit)
    val results: ArrayList<MBRecord>
        get() = records

    fun loadResults(obj: XOSRFObject): SearchResults {
        clearResults()
        visible = OSRFUtils.parseInt(obj["count"]) ?: 0
        if (visible == 0) return EvergreenSearchResults()

        // add to existing array, because SearchResultsFragment has an Adapter on it
        records.addAll(MBRecord.makeArrayFromQueryResults(obj))
        return EvergreenSearchResults()
    }

    fun clearResults() {
        records.clear()
        visible = 0
    }
}

class EvergreenSearchResults: SearchResults {
    override val numResults: Int
        get() = EgSearch.results.size

    override val totalMatches: Int
        get() = EgSearch.visible

    override val records: List<BibRecord>
        get() = EgSearch.results
}
