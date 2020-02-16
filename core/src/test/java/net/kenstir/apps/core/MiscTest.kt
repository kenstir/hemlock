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

package net.kenstir.apps.core

import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.evergreen_ils.utils.getCustomMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

class MiscTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    @Test
    fun test_Exception_customMessage_basic() {
        assertEquals("Timeout", java.util.concurrent.TimeoutException().getCustomMessage())
        assertEquals("Operation timed out after 15 seconds", com.android.volley.TimeoutError().getCustomMessage())
        assertEquals("Cancelled", java.lang.Exception().getCustomMessage())
        assertEquals("Cancelled", java.lang.Exception("").getCustomMessage())
    }

    // During system maintenance, the server can be up but responding 404 to gateway URLs.
    @Test
    fun test_Exception_customMessage_volleyNotFound() {
        assertEquals("Unknown client error.  The server may be offline.",
                com.android.volley.ClientError().getCustomMessage())

        val data = byteArrayOf(0x20, 0x0d, 0x0a)
        val response = com.android.volley.NetworkResponse(404, data, false, 200L, null)
        val ex = com.android.volley.ClientError(response)
        assertEquals("Not found.  The server may be down for maintenance.",
                ex.getCustomMessage())
    }
}
