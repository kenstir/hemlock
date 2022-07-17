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

package org.evergreen_ils.test

import org.evergreen_ils.OSRFUtils
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.JSONReader
import org.opensrf.util.OSRFRegistry

class JSONReaderTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val fields = arrayOf("can_haz_bacon","id","name")
            OSRFRegistry.registerObject("test", OSRFRegistry.WireProtocol.ARRAY, fields)
        }
    }

    @Test
    fun test_decode_wireObjectHasFewerFields() {
        val json = """
            {"__c":"test","__p":["t",1]}
            """
        val obj = JSONReader(json).readObject()
        assertEquals(true, OSRFUtils.parseBoolean(obj["can_haz_bacon"]))
        assertEquals(1, obj["id"])
    }

    @Test
    fun test_decode_wireObjectHasMoreFields() {
        val json = """
            {"__c":"test","__p":["t",1,"Hormel","Unexpected"]}
            """
        val obj = JSONReader(json).readObject()
        assertEquals(true, OSRFUtils.parseBoolean(obj["can_haz_bacon"]))
        assertEquals(1, obj["id"])
        assertEquals("Hormel", obj["name"])
    }
}
