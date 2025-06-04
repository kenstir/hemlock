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
package net.kenstir.hemlock.data.models

class PatronList(
    val id: Int,
    val name: String,
    val description: String,
    val public: Boolean = false,
) {
    override fun toString(): String {
        return "PatronList(id=$id, name='$name', description=$description, public=$public, items=${items?.size ?: 0})"
    }

    var items: List<PatronListItem>? = null
    var filterToVisibleRecords = false
    var visibleRecordIds = ArrayList<Int>() // list of bre IDs used to filter out deleted items
}
