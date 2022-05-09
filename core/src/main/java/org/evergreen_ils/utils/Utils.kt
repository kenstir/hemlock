/*
 * Copyright (c) 2019 Kenneth H. Cox
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

package org.evergreen_ils.utils

fun Any.fromApiToIntOrNull(): Int? {
    when (this) {
        is Int -> {
            return this
        }
        is String -> {
            return this.toIntOrNull()
        }
        else -> {
            return null
        }
    }
}

// Given a pubdate like "2000", "c2002", "2003-", or "2007-2014",
// extract the first number as an Int for sorting.
fun pubdateComparator(pubdate: String?): Int? {
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
