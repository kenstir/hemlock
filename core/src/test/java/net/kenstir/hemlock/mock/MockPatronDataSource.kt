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

package net.kenstir.hemlock.mock

import net.kenstir.hemlock.data.model.PatronList
import net.kenstir.hemlock.data.model.ListItem
import net.kenstir.hemlock.data.model.Record

object MockPatronDataSource {
    fun getLists(): List<PatronList> {
        return listOf(
            PatronList(1, "Books to Read", ""),
            PatronList(2, "Movies to Watch", "")
        )
    }

    fun getItems(listId: Int): List<ListItem> {
        return when (listId) {
            1 -> listOf(
                ListItem(1, Record(253,"Ready Player Two", "Cline, Ernest"))
            )
            2 -> listOf(
                ListItem(2, Record(320, "The Matrix Revolutions", "Wachowski, Lana")),
                ListItem(3, Record(222, "The Avengers", "Chechik, Jeremiah")),
            )
            else -> emptyList()
        }
    }
}
