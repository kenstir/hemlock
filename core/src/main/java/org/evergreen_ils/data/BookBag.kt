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

import org.opensrf.util.OSRFObject
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class BookBag(@JvmField val id: Int, obj: OSRFObject) : Serializable {
    @JvmField
    var name: String? = null
    @JvmField
    var description: String? = null
    var shared: Boolean? = null
    @JvmField
    var items = ArrayList<BookBagItem>()

    init {
        name = obj.getString("name")
        description = obj.getString("description")
        items = ArrayList()
        shared = obj.getBoolean("pub")
    }

    fun fleshFromObject(cbrebObj: OSRFObject) {
        items.clear()
        val fleshedItems = cbrebObj.get("items") as? ArrayList<OSRFObject> ?: ArrayList()
        for (item in fleshedItems) {
            items.add(BookBagItem(item))
        }
    }

    companion object {
        fun makeArray(objArray: List<OSRFObject>): ArrayList<BookBag> {
            val ret = ArrayList<BookBag>()
            for (obj in objArray) {
                obj.getInt("id")?.let { id ->
                    val bookBag = BookBag(id, obj)
                    ret.add(bookBag)
                }
            }
            return ret
        }
    }
}
