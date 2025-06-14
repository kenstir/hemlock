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

import android.text.TextUtils
import net.kenstir.hemlock.data.evergreen.OSRFUtils
import org.evergreen_ils.data.Organization
import org.evergreen_ils.data.MBRecord
import org.opensrf.util.OSRFObject
import kotlin.collections.ArrayList

object EgSearch {
    var selectedOrganization: Organization? = null
    var visible = 0
    var searchLimit = 100
    val results: ArrayList<MBRecord> = ArrayList(searchLimit)

    private val TAG = EgSearch::class.java.simpleName

    fun loadResults(obj: OSRFObject) {
        clearResults()
        visible = OSRFUtils.parseInt(obj["count"]) ?: 0
        if (visible == 0) return

        // parse ids list
        val record_ids_lol = obj["ids"] as List<List<*>>

        // add to existing array, because SearchResultsFragment has an Adapter on it
        results.addAll(MBRecord.makeArray(record_ids_lol))
    }

    // Build query string, taken with a grain of salt from
    // https://wiki.evergreen-ils.org/doku.php?id=documentation:technical:search_grammar
    // e.g. "title:Harry Potter chamber of secrets search_format(book) site(MARLBORO)"
    fun makeQueryString(searchText: String?, searchClass: String?, searchFormat: String?, sort: String?): String {
        val sb = StringBuilder()
        sb.append(searchClass).append(":").append(searchText)
        if (!TextUtils.isEmpty(searchFormat)) sb.append(" search_format(").append(searchFormat).append(")")
        if (selectedOrganization != null) sb.append(" site(").append(selectedOrganization!!.shortname).append(")")
        if (!TextUtils.isEmpty(sort)) sb.append(" sort(").append(sort).append(")")
        return sb.toString()
    }

    fun clearResults() {
        results.clear()
        visible = 0
    }
}
