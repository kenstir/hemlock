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

package org.evergreen_ils.data.service

import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.service.BiblioService
import net.kenstir.data.service.ImageSize
import net.kenstir.util.requireType
import org.evergreen_ils.Api
import org.evergreen_ils.data.model.EvergreenCopyCount
import org.evergreen_ils.data.model.MBRecord
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.gateway.paramListOf

object EvergreenBiblioService: BiblioService {

    override fun imageUrl(record: BibRecord, size: ImageSize): String? {
        return when (size) {
            ImageSize.SMALL -> GatewayClient.getUrl("/opac/extras/ac/jacket/small/r/" + record.id)
            ImageSize.MEDIUM -> GatewayClient.getUrl("/opac/extras/ac/jacket/medium/r/" + record.id)
            ImageSize.LARGE -> GatewayClient.getUrl("/opac/extras/ac/jacket/large/r/" + record.id)
        }
    }

    override suspend fun loadRecordDetails(bibRecord: BibRecord, needMARC: Boolean): Result<Unit> {
        return try {
            val record: MBRecord = bibRecord.requireType()
            Result.Success(loadRecordDetailsImpl(record, needMARC))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadRecordAttributes(bibRecord: BibRecord): Result<Unit> {
        return try {
            val record: MBRecord = bibRecord.requireType()
            val mraObj = fetchMRA(record.id)
            record.updateFromMRAResponse(mraObj)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadRecordCopyCounts(bibRecord: BibRecord, orgId: Int): Result<Unit> {
        return try {
            val record: MBRecord = bibRecord.requireType()
            Result.Success(loadRecordCopyCountImpl(record, orgId))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadRecordDetailsImpl(record: MBRecord, needMARC: Boolean) {
        record.mvrObj = fetchRecordMODS(record.id)

        if (needMARC) {
            val breObj = fetchMARC(record.id)
            record.updateFromBREResponse(breObj)
        }
    }

    suspend fun loadRecordCopyCountImpl(record: MBRecord, orgId: Int) {
        if (record.copyCounts != null) return

        val response = GatewayClient.fetch(Api.SEARCH, Api.COPY_COUNT, paramListOf(orgId, record.id), false)
        val copyCounts = response.payloadFirstAsObjectList()
        record.copyCounts = EvergreenCopyCount.makeArray(copyCounts)
    }

    suspend fun fetchCopyMODS(copyId: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.SEARCH, Api.MODS_FROM_COPY, paramListOf(copyId), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchRecordMODS(id: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.SEARCH, Api.MODS_SLIM_RETRIEVE, paramListOf(id), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchMetarecordMODS(id: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.SEARCH, Api.METARECORD_MODS_SLIM_RETRIEVE, paramListOf(id), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchMARC(id: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.PCRUD, Api.RETRIEVE_BRE, paramListOf(Api.ANONYMOUS, id), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchMRA(id: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.PCRUD, Api.RETRIEVE_MRA, paramListOf(Api.ANONYMOUS, id), true)
        return response.payloadFirstAsObject()
    }
}
