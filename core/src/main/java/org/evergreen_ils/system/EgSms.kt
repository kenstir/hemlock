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

package org.evergreen_ils.system

import net.kenstir.hemlock.logging.Log
import org.evergreen_ils.data.SMSCarrier
import org.evergreen_ils.xdata.XOSRFObject
import org.opensrf.util.OSRFObject


object EgSms {
    @JvmStatic
    var carriers = mutableListOf<SMSCarrier>()

    private const val TAG = "EgSms"

    fun loadCarriers(carriers: List<XOSRFObject>) {
        synchronized(this) {
            EgSms.carriers.clear()
            for (obj in carriers) {
                val id = obj.getInt("id")
                val name = obj.getString("name")
                if (id != null && name != null) {
                    EgSms.carriers.add(SMSCarrier(id, name))
                    Log.v(TAG, "loadSMSCarriers id:$id name:$name")
                }
            }
            EgSms.carriers.sort()
        }
    }

    @JvmStatic
    fun findCarrier(id: Int): SMSCarrier? = carriers.firstOrNull { it.id == id }
}
