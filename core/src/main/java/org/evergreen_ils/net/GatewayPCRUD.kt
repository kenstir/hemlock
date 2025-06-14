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

package org.evergreen_ils.net

import net.kenstir.hemlock.data.evergreen.Api
import org.evergreen_ils.system.EgCodedValueMap
import net.kenstir.hemlock.data.Result
import org.opensrf.util.OSRFObject

object GatewayPCRUD: PCRUDService {
    override suspend fun fetchCodedValueMaps(): Result<List<OSRFObject>> {
        return try {
            val formats = arrayListOf(EgCodedValueMap.ICON_FORMAT, EgCodedValueMap.SEARCH_FORMAT)
            val searchParams = mapOf<String, Any?>("ctype" to formats)
            val ret = Gateway.fetchObjectArray(Api.PCRUD, Api.SEARCH_CCVM, arrayOf<Any?>(Api.ANONYMOUS, searchParams), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchMARC(id: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.PCRUD, Api.RETRIEVE_BRE, arrayOf(Api.ANONYMOUS, id), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchMRA(id: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.PCRUD, Api.RETRIEVE_MRA, arrayOf(Api.ANONYMOUS, id), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchSMSCarriers(): Result<List<OSRFObject>> {
        return try {
            val searchParams = mapOf<String, Any?>("active" to 1)
            val ret = Gateway.fetchObjectArray(Api.PCRUD, Api.SEARCH_SMS_CARRIERS, arrayOf(Api.ANONYMOUS, searchParams), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
