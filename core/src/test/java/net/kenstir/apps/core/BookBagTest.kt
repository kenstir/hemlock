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

package net.kenstir.apps.core

import org.evergreen_ils.data.BookBag
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.xdata.XOSRFObject
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class BookBagTest {

    val cbrebObj = XOSRFObject(
        jsonMapOf(
            "id" to 24919,
            "name" to "books to read",
            "description" to null,
            "pub" to "t",
            "items" to null
        )
    )

    init {
        println("BookBagTest init ...")
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUp() {
            println("BookBagTest setUp ...")
        }
    }

    @Test
    fun test_makeArray() {
        val bookBags = BookBag.makeArray(arrayListOf(cbrebObj))
        assertEquals(1, bookBags.size)
        assertEquals("books to read", bookBags.first().name)
        assertEquals("", bookBags.first().description)
        assertTrue(bookBags.first().public)
    }

    @Test
    fun test_fleshFromObject() {
        val patronList = BookBag.makeArray(listOf(cbrebObj)).first()
        assertEquals("books to read", patronList.name)
        assertEquals(0, patronList.items.size)

        // a fleshed cbreb object has a non-empty items
        val map = cbrebObj.cloneMap()
        map["items"] = arrayListOf(
            jsonMapOf(
                "bucket" to 961216,
                "create_time" to "2020-01-11T10:31:44-0500",
                "pos" to null,
                "id" to 51454078,
                "target_biblio_record_entry" to 2914107
            )
        )
        val fleshedCbrebObj = XOSRFObject(map)

        val bookBag = patronList as BookBag
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(1, patronList.items.size)
        assertEquals(2914107, patronList.items.first().targetId)
    }

    @Test
    fun test_filterToVisibleRecords() {
        val patronList = BookBag.makeArray(listOf(cbrebObj)).first()
        assertEquals("books to read", patronList.name)
        assertEquals(0, patronList.items.size)
        val bookBag = patronList as BookBag

        val recordId = 2914107
        val queryPayload = XOSRFObject(
            jsonMapOf(
                "count" to 1,
                "ids" to arrayListOf(
                    arrayListOf(recordId, "2", "4.0")
                ))
        )
        val emptyQueryPayload = XOSRFObject(
            jsonMapOf(
                "count" to 0,
                "ids" to arrayListOf<ArrayList<Any?>>()
            )
        )

        // a fleshed cbreb object has a non-empty items
        val map = cbrebObj.cloneMap()
        map["items"] = arrayListOf(
            jsonMapOf(
                "bucket" to 961216,
                "create_time" to "2020-01-11T10:31:44-0500",
                "pos" to null,
                "id" to 51454078,
                "target_biblio_record_entry" to recordId
            )
        )
        val fleshedCbrebObj = XOSRFObject(map)

        // case 1: recordId is in the visible list
        bookBag.initVisibleIdsFromQuery(queryPayload)
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(1, bookBag.items.size)
        assertEquals(recordId, bookBag.items.first().targetId)

        // case 2: recordId is not in the visible list
        bookBag.initVisibleIdsFromQuery(emptyQueryPayload)
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(0, bookBag.items.size)
    }
}

fun XOSRFObject.cloneMap(): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for ((key, value) in this.map) {
        map[key] = value
    }
    return map
}
