/*
 * Copyright (c) 2020 Kenneth H. Cox
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
package org.evergreen_ils.data

import net.kenstir.hemlock.data.model.ListItem
import net.kenstir.hemlock.data.model.PatronList
import org.evergreen_ils.xdata.XOSRFObject
import kotlin.collections.ArrayList

class BookBag(
    override val id: Int,
    override val name: String,
    obj: XOSRFObject
): PatronList {
    override var description: String = obj.getString("description") ?: ""
    override val public: Boolean = obj.getBoolean("pub") ?: false

    override var items: List<ListItem> = ArrayList()

    var filterToVisibleRecords = false
    var visibleRecordIds = ArrayList<Int>() // list of bre IDs used to filter out deleted items

    fun initVisibleIdsFromQuery(multiclassQueryObj: XOSRFObject) {
        filterToVisibleRecords = true

        // ids is a list of lists of [record_id, ?, ?], e.g.:
        // [[1471992,"2","4.0"]]
        val idList = multiclassQueryObj.get("ids") as? ArrayList<ArrayList<Any?>>
        visibleRecordIds.clear()
        idList?.mapNotNullTo(visibleRecordIds) { it[0] as? Int }
    }

    fun fleshFromObject(cbrebObj: XOSRFObject) {
        val newItems = ArrayList<ListItem>()
        val fleshedItems = cbrebObj.get("items") as? ArrayList<XOSRFObject> ?: ArrayList()
        val distinctItems = fleshedItems.distinctBy { it.getInt("target_biblio_record_entry") }
        for (item in distinctItems) {
            if (!filterToVisibleRecords) {
                newItems.add(BookBagItem(item))
            } else {
                val targetId = item.getInt("target_biblio_record_entry")
                if (visibleRecordIds.find { it == targetId } != null) {
                    newItems.add(BookBagItem(item))
                }
            }
        }
        this.items = newItems
    }

    companion object {
        fun makeArray(objArray: List<XOSRFObject>): List<PatronList> {
            val ret = ArrayList<BookBag>()
            for (obj in objArray) {
                val id = obj.getInt("id")
                val name = obj.getString("name")
                if (id != null && name != null) {
                    val bookBag = BookBag(id, name, obj)
                    ret.add(bookBag)
                }
            }
            return ret
        }
    }
}
