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

package net.kenstir.hemlock.util

import net.kenstir.util.pubdateSortKey
import net.kenstir.util.titleSortKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilsTest {
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
