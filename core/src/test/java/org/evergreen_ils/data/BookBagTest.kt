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

package org.evergreen_ils.data

import net.kenstir.data.jsonMapOf
import net.kenstir.data.model.PatronList
import org.evergreen_ils.data.model.BookBag
import org.evergreen_ils.gateway.OSRFObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class BookBagTest {

    val cbrebObj = OSRFObject(
        jsonMapOf(
            "id" to 24919,
            "name" to "books to read",
            "description" to null,
            "pub" to "t",
            "items" to null
        ), "cbreb"
    )
    val targetRecordId = 2914107
    lateinit var fleshedCbrebObj: OSRFObject

    init {
        // a fleshed cbreb object has a non-empty list of items
        val map = cbrebObj.cloneMap()
        map["items"] = arrayListOf(
            OSRFObject(
                jsonMapOf(
                    "bucket" to 961216,
                    "create_time" to "2020-01-11T10:31:44-0500",
                    "pos" to null,
                    "id" to 51454078,
                    "target_biblio_record_entry" to targetRecordId
                ), "cbrebi"
            )
        )
        fleshedCbrebObj = OSRFObject(map)
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

        val bookBag = patronList as BookBag
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(1, patronList.items.size)
        assertEquals(targetRecordId, patronList.items.first().targetId)
    }

    @Test
    fun test_filterToVisibleRecords() {
        val patronList = BookBag.makeArray(listOf(cbrebObj)).first()
        assertEquals("books to read", patronList.name)
        assertEquals(0, patronList.items.size)
        val bookBag = patronList as BookBag

        val queryPayload = OSRFObject(
            jsonMapOf(
                "count" to 1,
                "ids" to arrayListOf(
                    arrayListOf(targetRecordId, "2", "4.0")
                ))
        )
        val emptyQueryPayload = OSRFObject(
            jsonMapOf(
                "count" to 0,
                "ids" to arrayListOf<ArrayList<Any?>>()
            )
        )

        // case 1: recordId is in the visible list
        bookBag.initVisibleIdsFromQuery(queryPayload)
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(1, bookBag.items.size)
        assertEquals(targetRecordId, bookBag.items.first().targetId)

        // case 2: recordId is not in the visible list
        bookBag.initVisibleIdsFromQuery(emptyQueryPayload)
        bookBag.fleshFromObject(fleshedCbrebObj)
        assertEquals(0, bookBag.items.size)
    }

    @Test
    fun test_skeletonBookbagIsSerializable() {
        val bookBag = BookBag(1, "Test Bag", cbrebObj)
        val serialized = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(serialized).use { it.writeObject(bookBag) }
        val deserialized = java.io.ByteArrayInputStream(serialized.toByteArray())
        val restored = java.io.ObjectInputStream(deserialized).use { it.readObject() as BookBag }
        println("Restored BookBag: $restored")
    }

    @Test
    fun test_fleshedBookbagIsSerializable() {
        val bookBag = BookBag(1, "Test Bag", cbrebObj)
        bookBag.fleshFromObject(fleshedCbrebObj)
        val serialized = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(serialized).use { it.writeObject(bookBag) }
        val deserialized = java.io.ByteArrayInputStream(serialized.toByteArray())
        val restored = java.io.ObjectInputStream(deserialized).use { it.readObject() as PatronList }
        println("Restored BookBag: $restored")
    }
}

fun OSRFObject.cloneMap(): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for ((key, value) in this.map) {
        map[key] = value
    }
    return map
}
