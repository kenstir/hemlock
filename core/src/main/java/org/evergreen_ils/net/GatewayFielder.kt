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

package org.evergreen_ils.net

import net.kenstir.hemlock.data.evergreen.Api
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import org.opensrf.util.OSRFObject

object GatewayFielder: FielderService {
    override suspend fun fetchBMP(holdTarget: Int): Result<OSRFObject> {
        return try {
            val param = jsonMapOf(
                    "cache" to 1,
                    "fields" to arrayListOf("label", "record"),
                    "query" to jsonMapOf("id" to holdTarget)
            )
            val ret = Gateway.fetchObjectArray(Api.FIELDER, Api.FIELDER_BMP_ATOMIC, arrayOf(param), false)
            Result.Success(ret.first())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
