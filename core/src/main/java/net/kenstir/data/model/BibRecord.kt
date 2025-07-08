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

import org.evergreen_ils.data.MARCRecord
import java.io.Serializable

interface BibRecord: Serializable {
    val id: Int
    val title: String
    val author: String
    val pubdate: String
    val publishingInfo: String
    val description: String
    val synopsis: String
    val isbn: String
    val titleSortKey: String
    val nonFilingCharacters: Int?
    val series: String
    val subject: String
    val iconFormatLabel: String

    var copyCounts: ArrayList<CopyCount>?
    var marcRecord: MARCRecord?
    var isDeleted: Boolean

    fun hasAttributes(): Boolean
    fun hasMarc(): Boolean
    fun hasMetadata(): Boolean
    fun getAttr(attrName: String?): String?
}
