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
package org.evergreen_ils.system

import org.evergreen_ils.xdata.XOSRFObject
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.system.EgCodedValueMap.ALL_SEARCH_FORMATS
import org.evergreen_ils.system.EgCodedValueMap.iconFormatLabel
import org.evergreen_ils.system.EgCodedValueMap.searchFormatCode
import org.evergreen_ils.system.EgCodedValueMap.searchFormatLabel
import org.evergreen_ils.system.EgCodedValueMap.searchFormatSpinnerLabels
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CodedValueMapTest {

    @Before
    fun setUp() {
        val objects = ArrayList<XOSRFObject>()
        run {
            val obj = XOSRFObject(
                jsonMapOf(
                    "ctype" to EgCodedValueMap.SEARCH_FORMAT,
                    "opac_visible" to true,
                    "code" to "book",
                    "value" to "Book (All)",
                )
            )
            objects.add(obj)
        }
        run {
            val obj = XOSRFObject(
                jsonMapOf(
                    "ctype" to EgCodedValueMap.ICON_FORMAT,
                    "opac_visible" to true,
                    "code" to "book",
                    "value" to "Book",
                )
            )
            objects.add(obj)
        }
        EgCodedValueMap.loadCodedValueMaps(objects)
    }

    @Test
    fun test_basic() {
        assertNull(searchFormatLabel("missing"))
        assertNull(searchFormatCode("Missing"))
        assertEquals("", searchFormatCode(null))
        assertEquals("", searchFormatCode(""))
        assertEquals("", searchFormatCode(ALL_SEARCH_FORMATS))
        assertEquals("Book (All)", searchFormatLabel("book"))
        assertEquals("Book", iconFormatLabel("book"))
        val labels = searchFormatSpinnerLabels
        assertEquals(ALL_SEARCH_FORMATS, labels[0])
    }

    @Test
    fun test_nulls() {
        assertNull(iconFormatLabel(null))
    }
}
