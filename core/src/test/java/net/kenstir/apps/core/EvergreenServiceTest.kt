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

import org.evergreen_ils.system.EgSms
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.OSRFObject

class EvergreenServiceTest {

    fun makeObj(map: Map<String, Any?>): OSRFObject {
        return OSRFObject(map)
    }

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

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }
}
