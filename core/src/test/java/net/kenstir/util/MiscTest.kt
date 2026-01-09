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

package net.kenstir.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class MiscTest {

    private fun testString(name: String): String? {
        return when (name) {
            "(null)" -> null
            "(empty)" -> ""
            else -> name
        }
    }

    /** Test to convince myself that I don't need TextUtils.equals() to test equality of optional Strings */
    @Test
    fun test_optionalStringEquals() {
        val a = testString("hello")
        val b = testString("hello")
        val c = testString("(null)")
        val d = testString("(null)")
        val e = d
        assertTrue(a == b)
        assertTrue(a != c)
        assertTrue(c != a)
        assertTrue(c == d)
        assertTrue(c == e)
    }

    @Test
    fun test_cancellation_exception() {
        val ce = CancellationException("Test cancellation")
        var caughtWhere = 0
        try {
            throw ce
        } catch (e: Exception) {
            caughtWhere = 1
        }
        assertEquals(1, caughtWhere)
    }
}
