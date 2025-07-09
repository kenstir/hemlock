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

package org.evergreen_ils.system

import net.kenstir.data.jsonMapOf
import org.evergreen_ils.gateway.OSRFObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EgSmsTest {

    fun make_csc_obj(id: Int, name: String): OSRFObject {
        return OSRFObject(jsonMapOf("id" to id, "name" to name))
    }

    @Test
    fun test_loadSMSCarriers() {
        val carriers = arrayListOf(
            make_csc_obj(48, "T-Mobile"),
            make_csc_obj(52, "Sprint (PCS)")
        )

        EgSms.loadCarriers(carriers)

        assertNull(EgSms.findCarrier(1))
        assertEquals("T-Mobile", EgSms.findCarrier(48)?.name)
    }
}
