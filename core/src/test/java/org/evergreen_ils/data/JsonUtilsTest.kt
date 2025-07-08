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

import net.kenstir.util.JsonUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonUtilsTest {

    @Test
    fun test_parseObjectOK() {
        val json = """
            {"_attr":"mr_hold_format","_val":"book"}
            """
        val obj = JsonUtils.parseObject(json)
        assertEquals("book", obj?.get("_val"))
    }

    @Test
    fun test_parseNonObject() {
        assertNull(JsonUtils.parseObject(""""bare string""""))
        assertNull(JsonUtils.parseObject("8"))
        assertNull(JsonUtils.parseObject("[42]"))
        assertNull(JsonUtils.parseObject(""))
        assertNull(JsonUtils.parseObject(null))
    }
}
