/*
 * Copyright (c) 2019 Kenneth H. Cox
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
//
//import org.evergreen_ils.Api
//import org.evergreen_ils.system.Log
//import org.evergreen_ils.system.StdoutLogProvider
//import org.junit.Assert
//import org.junit.BeforeClass
//import org.junit.Test
//import org.opensrf.util.OSRFObject
//import org.opensrf.util.OSRFRegistry
//
//class OSRFObjectTest {
//
//    companion object {
//        @BeforeClass
//        @JvmStatic
//        fun setUpClass() {
//            Log.setProvider(StdoutLogProvider())
//            val fields = arrayOf("juvenile","usrname","home_ou")
//            OSRFRegistry.registerObject("au", OSRFRegistry.WireProtocol.ARRAY, fields)
//        }
//    }
//
//    @Test
//    fun test_basic() {
//        val json = """
//            {"status":200,"payload":[{"__c":"au","__p":["f","luser",69]}]}
//            """
//        val obj = OSRFObject()
//        Assert.assertNotNull(obj)
//        Assert.assertEquals(false, Api.parseBoolean(obj.get("juvenile")))
//        Assert.assertEquals("luser", obj.getString("usrname"))
//        Assert.assertEquals(69, obj.getInt("home_ou"))
//    }
//}