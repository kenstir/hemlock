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

package net.kenstir.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MiscExtensionsTest {

    @Test
    fun test_Exception_customMessage() {
        assertEquals("Timeout", java.util.concurrent.TimeoutException().getCustomMessage())
        assertEquals("Cancelled", java.util.concurrent.CancellationException().getCustomMessage())
        assertEquals("Cancelled", java.lang.Exception().getCustomMessage())
        assertEquals("Cancelled", java.lang.Exception("").getCustomMessage())

        // the nested exception [ex3] happens if you click away from the AccountManager account picker
        val ex1 = java.util.concurrent.CancellationException()
        val ex2 = android.accounts.OperationCanceledException(null, ex1)
        val ex3 = java.lang.IllegalStateException(null, ex2)
        assertEquals("Cancelled", ex3.getCustomMessage())
        assertEquals("Cancelled", ex2.getCustomMessage())
        assertEquals("Cancelled", ex1.getCustomMessage())
    }

    @Test
    fun test_Exception_isServerOffline() {
        // case: nginx is down, got: java.net.ConnectException: Failed to connect to ...
        assertEquals(true, java.net.ConnectException("Failed to connect to HOST/IP:PORT").isServerOfflineError())

        // case: nginx is up, evergreen down, got: java.io.InterruptedIOException: timeout
        assertEquals(true, java.io.InterruptedIOException("timeout").isServerOfflineError())

        // case seen in all the Evergreen Indiana reports
        val msg = "Connect timeout has expired [url=https://evergreen.example.org/osrf-gateway-v1, connect_timeout=unknown ms]"
        assertEquals(true, io.ktor.client.network.sockets.ConnectTimeoutException(msg).isServerOfflineError())

        // server not offline
        assertEquals(false, null.isServerOfflineError())
    }

    @Test
    fun test_indexOfOrZero() {
        val list = listOf("a", "b", "c")
        assertEquals(0, list.indexOfOrZero("a"))
        assertEquals(1, list.indexOfOrZero("b"))
        assertEquals(2, list.indexOfOrZero("c"))
        assertEquals(0, list.indexOfOrZero("d"))
    }

    @Test
    fun test_indexOfOrZeroArray() {
        val list = arrayOf("a", "b", "c")
        assertEquals(0, list.indexOfOrZero("a"))
        assertEquals(1, list.indexOfOrZero("b"))
        assertEquals(2, list.indexOfOrZero("c"))
        assertEquals(0, list.indexOfOrZero("d"))
    }

    @Test
    fun test_indexOfFirstOrZero() {
        val list = listOf("a", "b", "c")
        assertEquals(0, list.indexOfFirstOrZero { it == "a" })
        assertEquals(1, list.indexOfFirstOrZero { it == "b" })
        assertEquals(2, list.indexOfFirstOrZero { it == "c" })
        assertEquals(0, list.indexOfFirstOrZero { it == "d" })
    }
}
