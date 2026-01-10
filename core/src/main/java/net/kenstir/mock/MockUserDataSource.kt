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

package net.kenstir.mock

object MockUserDataSource {
    var counter: Int = 0

//    fun getLists(): List<PatronList> {
//        return listOf(
//            PatronList(1, "Books to Read", ""),
//            PatronList(2, "Movies to Watch", "")
//        )
//    }
//
//    fun makeListItem(id: Int, title: String, author: String): ListItem {
//        val obj = MBRecord(OSRFObject(jsonMapOf(
//            "doc_id" to id,
//            "title" to title,
//            "author" to author
//        )))
//        return ListItem(id, obj)
//    }
//
//    fun getItems(listId: Int): List<ListItem> {
//        return when (listId) {
//            1 -> listOf(
//                makeListItem(253,"Ready Player Two", "Cline, Ernest")
//            )
//            2 -> listOf(
//                makeListItem(320, "The Matrix Revolutions", "Wachowski, Lana"),
//                makeListItem(222, "The Avengers", "Chechik, Jeremiah")
//            )
//            else -> emptyList()
//        }
//    }
}
