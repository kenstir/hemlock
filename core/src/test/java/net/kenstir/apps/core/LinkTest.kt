/*
 * Copyright (c) 2019 Kenneth H. Cox
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

import org.evergreen_ils.utils.Link
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkTest {
    @Test
    fun test_equals() {
        val a = Link("http://google.com", "Link to somewhere")
        val b = Link("http://google.com", "Link to somewhere")
        val c = Link("http://google.com", "Different text same href")
        assertTrue(a == b)
        assertTrue(a != c)
        assertFalse(a == c)
        assertFalse(b == c)
    }
}