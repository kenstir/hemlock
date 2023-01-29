/*
 * Copyright (c) 2022 Kenneth H. Cox
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

import org.evergreen_ils.data.jsonMapOf
import org.evergreen_ils.utils.fromApiToIntOrNull
import org.evergreen_ils.utils.pubdateSortKey
import org.evergreen_ils.utils.titleSortKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilsTest {
    @Test
    fun test_fromApiToIntOrNull() {
        val map = jsonMapOf("id" to 1)
        assertEquals(null, map.fromApiToIntOrNull())
        assertEquals(42, 42.fromApiToIntOrNull())
        assertEquals(42, "42".fromApiToIntOrNull())
    }

    @Test
    fun test_pubdateSortKey() {
        assertNull(pubdateSortKey(null))
        assertNull(pubdateSortKey("abcde"))
        assertEquals(2000, pubdateSortKey("2000"))
        assertEquals(2002, pubdateSortKey("c2002"))
        assertEquals(2003, pubdateSortKey("2003-"))
        assertEquals(2007, pubdateSortKey("2007-2014"))
    }

    @Test
    fun test_titleSortKey() {
        assertNull(titleSortKey(null))
        assertEquals("A\" IS FOR ALIBI", titleSortKey("\"A\" is for Alibi"))
        assertEquals("IS FOR ALIBI", titleSortKey("A is for Alibi"))
        assertEquals("CAT IN THE HAT", titleSortKey("The Cat in the Hat"))
        assertEquals("DIARY]", titleSortKey("[Diary]"))
    }
}
