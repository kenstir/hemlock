/*
 * Copyright (C) 2017 Kenneth H. Cox
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

import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgCodedValueMap.ALL_SEARCH_FORMATS
import org.evergreen_ils.system.EgCodedValueMap.iconFormatLabel
import org.evergreen_ils.system.EgCodedValueMap.loadCodedValueMaps
import org.evergreen_ils.system.EgCodedValueMap.searchFormatCode
import org.evergreen_ils.system.EgCodedValueMap.searchFormatLabel
import org.evergreen_ils.system.EgCodedValueMap.searchFormatSpinnerLabels
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class CodedValueMapTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass(): Unit {
            Log.setProvider(StdoutLogProvider())
        }
    }

    @Before
    fun setUp() {
        val objects = ArrayList<OSRFObject>()
        run {
            val obj = OSRFObject()
            obj["ctype"] = EgCodedValueMap.SEARCH_FORMAT
            obj["opac_visible"] = true
            obj["code"] = "book"
            obj["value"] = "Book (All)"
            objects.add(obj)
        }
        run {
            val obj = OSRFObject()
            obj["ctype"] = EgCodedValueMap.ICON_FORMAT
            obj["opac_visible"] = true
            obj["code"] = "book"
            obj["value"] = "Book"
            objects.add(obj)
        }
        loadCodedValueMaps(objects)
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
