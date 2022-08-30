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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package net.kenstir.apps.core

import org.evergreen_ils.OSRFUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OSRFUtilsTest {
    @Test
    fun test_parseInt() {
        assertNull(OSRFUtils.parseInt(null))

        assertEquals(42, OSRFUtils.parseInt(Integer(42)))
        assertEquals(42, OSRFUtils.parseInt("42"))

        assertNull(OSRFUtils.parseInt(""))
        assertNull(OSRFUtils.parseInt("null"))
        assertEquals(1, OSRFUtils.parseInt("", 1))
    }
}
