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

package net.kenstir.data.model

object SearchClass {
    const val KEYWORD = "keyword"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val SUBJECT = "subject"
    const val SERIES = "series"
    const val IDENTIFIER = "identifier"

    val spinnerLabels = listOf(
        "Keyword",
        "Title",
        "Author",
        "Subject",
        "Series",
        "ISBN or UPC"
    )
    val spinnerValues = listOf(
        KEYWORD,
        TITLE,
        AUTHOR,
        SUBJECT,
        SERIES,
        IDENTIFIER
    )
}
