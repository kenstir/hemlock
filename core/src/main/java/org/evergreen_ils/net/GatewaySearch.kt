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

import org.evergreen_ils.Api
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import org.opensrf.util.OSRFObject

object GatewaySearch: SearchService {
    override suspend fun fetchAssetCopy(copyId: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.SEARCH, Api.ASSET_COPY_RETRIEVE, arrayOf(copyId), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchAssetCallNumber(callNumber: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.SEARCH, Api.ASSET_CALL_NUMBER_RETRIEVE, arrayOf(callNumber), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchCopyLocationCounts(id: Int, orgId: Int, orgLevel: Int): Result<List<Any>> {
        return try {
            val args = arrayOf<Any?>(id, orgId, orgLevel)
            val ret = Gateway.fetch(Api.SEARCH, Api.COPY_LOCATION_COUNTS, args, false) {
                it.payloadFirstAsList()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchCopyStatuses(): Result<List<OSRFObject>> {
        return try {
            val ret = Gateway.fetchObjectArray(Api.SEARCH, Api.COPY_STATUS_ALL, arrayOf(), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchCopyCount(id: Int, orgId: Int): Result<List<OSRFObject>> {
        return try {
            val ret = Gateway.fetchObjectArray(Api.SEARCH, Api.COPY_COUNT, arrayOf(orgId, id), false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchCopyMODS(copyId: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.SEARCH, Api.MODS_FROM_COPY, arrayOf(copyId), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchRecordMODS(id: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.SEARCH, Api.MODS_SLIM_RETRIEVE, arrayOf(id), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchMetarecordMODS(id: Int): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.SEARCH, Api.METARECORD_MODS_SLIM_RETRIEVE, arrayOf(id), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchHoldParts(id: Int): Result<List<OSRFObject>> {
        return try {
            val param = jsonMapOf("record" to id)
            val ret = Gateway.fetchObjectArray(Api.SEARCH, Api.HOLD_PARTS, arrayOf(param), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

}
