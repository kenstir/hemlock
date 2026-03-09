/*
 * Copyright (c) 2026 Kenneth H. Cox
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

class StringExtensionsTest {

    @Test
    fun test_md5() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", "".md5())
        assertEquals("900150983cd24fb0d6963f7d28e17f72", "abc".md5())
    }

    @Test
    fun test_expandTemplate() {
        data class Case(val template: String, val values: Map<String, String>, val expected: String)

        val cases = listOf(
            Case("{baseUrl}/eg/opac/record/{recordId}#awards",
                mapOf("baseUrl" to "https://example.com", "recordId" to "1"),
                "https://example.com/eg/opac/record/1#awards"),
            Case("{id}?{id}#{id}", mapOf("id" to "1"), "1?1#1"),
            Case("beginning_{s}_end", mapOf("s" to "middle"), "beginning_middle_end"),
            Case("No tokens here", mapOf("id" to "1"), "No tokens here"),
        )

        for ((i, c) in cases.withIndex()) {
            val got = c.template.expandTemplate(c.values)
            assertEquals("case #$i failed (template=${c.template})", c.expected, got)
        }
    }

    @Test
    fun test_expandTemplate_missingKey() {
        val res = kotlin.runCatching { "{missing}".expandTemplate(emptyMap()) }
        assertTrue(res.isFailure)
    }
}
