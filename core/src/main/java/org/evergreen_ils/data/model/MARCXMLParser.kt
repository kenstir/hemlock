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

import net.kenstir.data.model.MARCRecord
import net.kenstir.logging.Log.logElapsedTime
import net.kenstir.data.model.MARCRecord.Companion.isDatafieldUseful
import net.kenstir.data.model.MARCRecord.MARCDatafield
import net.kenstir.data.model.MARCRecord.MARCSubfield
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class MARCXMLParser(private val inStream: InputStream) {
    var currentRecord: MARCRecord = MARCRecord()
    var currentDatafield: MARCDatafield? = null
    var currentSubfield: MARCSubfield? = null

    constructor(xml: String) : this(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))

    @Throws(Exception::class)
    fun parse(): MARCRecord {
        try {
            val start = System.currentTimeMillis()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(this.inStream, null)
            var eventType = parser.getEventType()

            // cycle through the XML events
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> handleStartElement(parser)
                    XmlPullParser.END_TAG -> handleEndElement(parser)
                    XmlPullParser.TEXT -> handleText(parser)
                }
                eventType = parser.next()
            }
            logElapsedTime(TAG, start, "marcxml.parse")
        } catch (ex: XmlPullParserException) {
            throw Exception("Error parsing MARCXML", ex)
        }

        return currentRecord
    }

    private fun handleStartElement(parser: XmlPullParser) {
        val ns: String? = null
        when (parser.name) {
            "datafield" -> {
                val tag = parser.getAttributeValue(ns, "tag")
                val ind1 = parser.getAttributeValue(ns, "ind1")
                val ind2 = parser.getAttributeValue(ns, "ind2")
                // We only care about certain tags
                // See also templates/opac/parts/misc_util.tt2
                // See also https://www.loc.gov/marc/bibliographic/bd856.html
                if (isDatafieldUseful(tag, ind1, ind2)) {
                    currentDatafield = MARCDatafield(tag, ind1, ind2)
                }
            }
            "subfield" -> {
                val code = parser.getAttributeValue(ns, "code")
                if (currentDatafield != null) {
                    currentSubfield = MARCSubfield(code)
                }
            }
        }
    }

    private fun handleEndElement(parser: XmlPullParser) {
        when (parser.name) {
            "datafield" -> currentDatafield?.let {
                currentRecord.datafields.add(it)
                currentDatafield = null
            }
            "subfield" -> currentDatafield?.let { df ->
                currentSubfield?.let { sf ->
                    df.subfields.add(sf)
                    currentSubfield = null
                }
            }
        }
    }

    private fun handleText(parser: XmlPullParser) {
        currentSubfield?.text = parser.text
    }

    companion object {
        private const val TAG = "MARCXMLParser"
    }
}
