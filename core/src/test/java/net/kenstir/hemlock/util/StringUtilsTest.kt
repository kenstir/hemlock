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

import org.junit.Assert
import org.junit.Test

class StringUtilsTest {

    @Test
    fun test_take() {
        val s1: String? = null
        Assert.assertEquals("", StringUtils.take(s1, 4))
        val s2 = "abcdef"
        Assert.assertEquals("abcd", StringUtils.take(s2, 4))
        Assert.assertEquals(s2, StringUtils.take(s2, 6))
        Assert.assertEquals(s2, StringUtils.take(s2, 8))
    }
}
