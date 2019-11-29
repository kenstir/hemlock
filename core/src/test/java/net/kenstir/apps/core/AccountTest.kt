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

import org.evergreen_ils.data.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class AccountTest {

    companion object {
        lateinit var sessionObjMap: Map<String, Any?>

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            sessionObjMap = mutableMapOf(
                    "id" to 42,
                    "home_ou" to 69,
                    "day_phone" to "508-555-1212"
            )
        }
    }

    @Test
    fun test_basic() {
        val account = Account("hemlock")
        assertEquals("hemlock", account.username)
        assertNull(account.authToken)
    }

    @Test
    fun test_loadFromObj() {
        val account = Account("hemlock", "636f7666656665")
        assertEquals("636f7666656665", account.authTokenOrThrow())

        account.loadSession(OSRFObject(sessionObjMap))
        assertEquals(42, account.id)
        assertEquals("508-555-1212", account.phoneNumber)
    }
}