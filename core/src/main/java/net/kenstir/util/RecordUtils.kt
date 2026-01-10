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

package net.kenstir.util

import android.content.res.Resources
import net.kenstir.data.model.BibRecord
import net.kenstir.hemlock.R
import net.kenstir.ui.App

// Given a pubdate like "2000", "c2002", "2003-", or "2007-2014",
// extract the first number as an Int for sorting.
fun pubdateSortKey(pubdate: String?): Int? {
    val s = pubdate ?: return null
    val startIndex = s.indexOfFirst { it.isDigit() }
    if (startIndex < 0) return null
    val s2 = s.substring(startIndex)
    val endIndex = s2.indexOfFirst { !it.isDigit() }
    val s3 = when {
        endIndex < 0 -> s2
        else -> s2.substring(0, endIndex)
    }
    return s3.toIntOrNull()
}

// Given a title, return a sort key
// This function is used in the absence of a MARC record
fun titleSortKey(title: String?): String? {
    val t = title ?: return null

    // uppercase and remove leading articles
    val t2 = t.uppercase()
        .removePrefix("A ")
        .removePrefix("AN ")
        .removePrefix("THE ")

    // filter out punctuation
    // modeled after code in misc_util.tt2 block get_marc_attrs
    return t2.replace("^[^A-Z0-9]*".toRegex(), "")
}

fun BibRecord.getCopySummary(resources: Resources, orgID: Int?): String {
    var total = 0
    var available = 0
    if (copyCounts == null) return ""
    if (orgID == null) return ""
    for (copyCount in copyCounts.orEmpty()) {
        if (copyCount.orgId == orgID) {
            total = copyCount.count
            available = copyCount.available
            break
        }
    }
    val totalCopies = resources.getQuantityString(R.plurals.number_of_copies, total, total)
    return String.format(resources.getString(R.string.n_of_m_available),
        available, totalCopies, App.svc.orgService.getOrgNameSafe(orgID))
}

/**
 * Returns true if the record is null or the record is a pre-cat
 *
 * meaning it does not have a full bib record and might be a loaned item
 * or just not fully cataloged.
 */
fun BibRecord?.isNullOrPreCat(): Boolean {
    if (this == null) return true
    if (this.id == -1) return true
    return false
}
