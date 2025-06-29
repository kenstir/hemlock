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

package org.evergreen_ils.net

import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.model.BibRecord
import net.kenstir.hemlock.net.BiblioService
import org.evergreen_ils.Api
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.XOSRFObject
import org.evergreen_ils.xdata.paramListOf

class EvergreenBiblioService: BiblioService {

    override suspend fun loadRecordDetails(bibRecord: BibRecord, needMARC: Boolean): Result<Unit> {
        val record = bibRecord as? MBRecord
            ?: throw IllegalArgumentException("Expected MBRecord, got ${bibRecord::class.java.simpleName}")

        return try {
            Result.Success(loadRecordDetailsImpl(record, needMARC))
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

        // TODO: fetch attributes (MRA)?
    }

    suspend fun fetchCopyMODS(copyId: Int): XOSRFObject {
        val response = XGatewayClient.fetch(Api.SEARCH, Api.MODS_FROM_COPY, paramListOf(copyId), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchRecordMODS(id: Int): XOSRFObject {
        val response = XGatewayClient.fetch(Api.SEARCH, Api.MODS_SLIM_RETRIEVE, paramListOf(id), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchMetarecordMODS(id: Int): XOSRFObject {
        val response = XGatewayClient.fetch(Api.SEARCH, Api.METARECORD_MODS_SLIM_RETRIEVE, paramListOf(id), true)
        return response.payloadFirstAsObject()
    }

    suspend fun fetchMARC(id: Int): XOSRFObject {
        val response = XGatewayClient.fetch(Api.PCRUD, Api.RETRIEVE_BRE, paramListOf(Api.ANONYMOUS, id), true)
        return response.payloadFirstAsObject()
    }
}
