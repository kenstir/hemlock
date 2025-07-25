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
package org.evergreen_ils

import androidx.test.platform.app.InstrumentationRegistry
import net.kenstir.logging.Log
import org.evergreen_ils.data.model.MARCRecord
import org.evergreen_ils.data.model.MARCXMLParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MARCXMLParserTest {

    @Before
    fun setUp() {
    }

    @Test
    fun test_marcrecord() {
        val marcRecord = MARCRecord()
        assertEquals(0, marcRecord.datafields.size.toLong())
    }

    @Test
    fun test_marcxml_partial() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val inStream = ctx.resources.assets.open("marcxml_acorn_partial_3185816.xml")
        val parser = MARCXMLParser(inStream)
        val marcRecord = parser.parse()

        // Only a subset of 856 tags are kept, see MARCXMLParser
        val datafields = marcRecord.datafields
        assertEquals(4, datafields.size)

        // First datafield has 4 subfields
        val df = datafields[0]
        val subfields = df.subfields
        assertEquals(4, subfields.size)

        // 4 links
        val links = marcRecord.links
        assertEquals(4, links.size)

        // First link is an Excerpt
        val (href, text) = links[0]
        assertEquals("Excerpt", text)
        assertEquals("https://samples.overdrive.com/hunger-games-c540fc?.epub-sample.overdrive.com", href)
    }

    @Test
    fun test_marcxml_wtf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val inStream = ctx.resources.assets.open("marcxml_mo_89986_partial.xml")
        val parser = MARCXMLParser(inStream)
        val marcRecord = parser.parse()

        // Only a subset of 856 tags are kept, see MARCXMLParser
        val datafields = marcRecord.datafields
        for (df in datafields) {
            Log.d("test", "df tag=${df.tag} ind1=${df.ind1} ind2=${df.ind2}")
            for (sf in df.subfields) {
                Log.d("test", ".. code=${sf.code} text=${sf.text}")
            }
        }
        assertEquals(5, datafields.size)
    }
}
