/*
 * Copyright (C) 2020 Kenneth H. Cox
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
package org.evergreen_ils.test

import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.evergreen_ils.data.MBRecord
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.GatewayResult
import org.opensrf.util.JSONReader
import org.opensrf.util.OSRFRegistry
import java.util.ArrayList

class MBRecordTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            val mvrFields = arrayOf("title","author","doc_id","doc_type","pubdate","isbn","publisher","tcn","subject","types_of_resource","call_numbers","edition","online_loc","synopsis","physical_description","toc","copy_count","series","serials","foreign_copy_maps")
            OSRFRegistry.registerObject("mvr", OSRFRegistry.WireProtocol.ARRAY, mvrFields)
        }
    }

    @Before
    fun setUp() {
    }

    @Test
    fun test_bareRecord() {
        val record = MBRecord(4600952)

        assertEquals(false, record.hasMetadata)
        assertEquals(4600952, record.id)
        assertEquals("", record.title)
        assertEquals("", record.subject)
    }

    @Test
    fun test_withMvrObj() {
        val json = """
            {"payload":[{"__c":"mvr","__p":["Black ops","Prado, Ric",4600952,null,"2022","9781250271846",null,"4600952",{"Employees":1,"United States. Central Intelligence Agency.":1},["text"],[],"First edition.",[],"\"A memoir\"","print xiv, 384 pages",null,null,[]]}],"status":200}
        """.trimIndent()
        val result = GatewayResult.create(json)
        val mvrObj = result.asObject()
        val record = MBRecord(4600952)
        record.updateFromMODSResponse(mvrObj)

        assertEquals(true, record.hasMetadata)
        assertEquals(4600952, record.id)
        assertEquals("Black ops", record.title)
        assertEquals("Prado, Ric", record.author)
        assertEquals("2022", record.pubdate)
        assertEquals("\"A memoir\"", record.synopsis)
        assertEquals("print xiv, 384 pages", record.physicalDescription)
        assertEquals("9781250271846", record.isbn)

        assertEquals("", record.series)
        // sorting is not stable
        //assertEquals("Employees\nUnited States. Central Intelligence Agency.", record.subject)

        assertEquals("", record.iconFormatLabel)
        assertNull(record.attrs)
        assertNull(record.copyCounts)
        assertNull(record.marcRecord)
        assertEquals(false, record.isDeleted)
    }

    private fun makeArrayFromJson(json: String): ArrayList<MBRecord> {
        val idsList = JSONReader(json).readArray() as List<List<*>?>
        return MBRecord.makeArray(idsList)
    }

    @Test
    fun test_makeArray1() {
        val json = """
            [[32673,null,"0.0"],[886843,null,"0.0"]]
            """
        val records = makeArrayFromJson(json)
        Assert.assertEquals(2, records.size.toLong())
        Assert.assertEquals(32673, records[0].id)
    }

    @Test
    fun test_makeArray2() {
        val json = """
            [["503610",null,"0.0"],["502717",null,"0.0"]]
            """
        val records = makeArrayFromJson(json)
        Assert.assertEquals(2, records.size.toLong())
        Assert.assertEquals(503610, records[0].id)
        Assert.assertEquals(502717, records[1].id)
    }

    @Test
    fun test_makeArray3() {
        val json = """ 
            [["1805532"],["2385399"]]
            """
        val records = makeArrayFromJson(json)
        Assert.assertEquals(2, records.size.toLong())
        Assert.assertEquals(1805532, records[0].id)
        Assert.assertEquals(2385399, records[1].id)
    }
}
