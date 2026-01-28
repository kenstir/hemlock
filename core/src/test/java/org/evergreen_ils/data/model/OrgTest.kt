/*
 * Copyright (c) 2026 Kenneth H. Cox
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

import net.kenstir.data.jsonMapOf
import org.evergreen_ils.gateway.OSRFObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OrgTest {
    var org: EvergreenOrganization? = null

    @Before
    fun setUp() {
        val br1 = OSRFObject(
            jsonMapOf(
                "id" to 4,
                "ou_type" to 3,
                "shortname" to "BR1",
                "name" to "Example Branch 1",
                "opac_visible" to "t",
                "parent_ou" to 2,
                "children" to null
            )
        )
        org = EvergreenOrganization(
            id = 4,
            level = 2,
            name = "Example Branch 1",
            shortname = "BR1",
            opacVisible = true,
            parent = 2,
            obj = br1
        )
    }

    @Test
    fun test_addressWithEmptyStreet2() {
        val addressObj = OSRFObject(
            jsonMapOf(
                "street1" to "123 Main St",
                "street2" to "",
                "city" to "Anytown",
                "state" to "CA",
                "post_code" to "90210",
                "country" to "USA"
            )
        )
        org?.loadAddress(addressObj)
        assertEquals("123 Main St, Anytown, CA 90210", org?.navigationAddress)
        assertEquals("123 Main St\nAnytown, CA 90210", org?.displayAddress)
    }

    @Test
    fun test_addressWithNullStreet2() {
        val addressObj = OSRFObject(
            jsonMapOf(
                "street1" to "123 Main St",
                "street2" to null,
                "city" to "Anytown",
                "state" to "CA",
                "post_code" to "90210",
                "country" to "USA"
            )
        )
        org?.loadAddress(addressObj)
        assertEquals("123 Main St, Anytown, CA 90210", org?.navigationAddress)
        assertEquals("123 Main St\nAnytown, CA 90210", org?.displayAddress)
    }
}
