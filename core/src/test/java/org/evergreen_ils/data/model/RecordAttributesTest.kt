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
package org.evergreen_ils.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordAttributesTest {

    @Test
    fun test_parseAttributes_basic() {
        val attrs = """
            "item_form"=>" ", "item_type"=>"a", "icon_format"=>"book", "content_type"=>"still image", "search_format"=>"book", "mr_hold_format"=>"book"
        """.trimIndent()
        val map = RecordAttributes.parseAttributes(attrs)
        assertEquals(6, map.size)
        assertEquals("book", map["icon_format"])
        assertEquals(" ", map["item_form"])
        assertEquals(null, map["xyzzy"])
    }

    /* from https://evergreen.cool-cat.org/osrf-gateway-v1?service=open-ils.pcrud&method=open-ils.pcrud.retrieve.mra&param=%22ANONYMOUS%22&param=1613894 */
    @Test
    fun test_parseAttributes_withEmbeddedComma() {
        val attrs = """
            "icon_format"=>"book", "marc21_biblio_300_sub_a"=>"xviii, 253 pages ;"
        """.trimIndent()
        val map = RecordAttributes.parseAttributes(attrs)
        assertEquals(2, map.size)
        assertEquals("book", map["icon_format"])
        assertEquals("xviii, 253 pages ;", map["marc21_biblio_300_sub_a"])
    }
}
