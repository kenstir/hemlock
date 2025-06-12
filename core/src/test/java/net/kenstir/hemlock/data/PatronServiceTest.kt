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

package net.kenstir.hemlock.data

import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.mock.MockPatronService
import org.junit.Assert.assertEquals
import org.junit.Test

class PatronServiceTest {
    @Test
    fun getLists() = runTest {
        val service = MockPatronService()
        val lists = service.fetchLists(1, "authtoken").get()
        assertEquals(2, lists.size)
        assertEquals("Books to Read", lists[0].name)
        assertEquals("Movies to Watch", lists[1].name)
        assertEquals(false, lists[0].isFullyLoaded)
    }

    @Test
    fun getItems() = runTest {
        val service = MockPatronService()
        val items = service.fetchListItems(1, "authtoken", 1).get()
        assertEquals(1, items.size)
        assertEquals(253, items[0].record.id)
    }
}
