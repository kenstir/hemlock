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

package org.evergreen_ils

import org.evergreen_ils.utils.JsonUtils
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
