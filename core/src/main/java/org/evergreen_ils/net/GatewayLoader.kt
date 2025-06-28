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

import net.kenstir.hemlock.data.Result
import org.evergreen_ils.data.*
import org.evergreen_ils.system.EgSms

object GatewayLoader {

    suspend fun loadRecordMetadataAsync(record: MBRecord): Result<Unit> {
//        if (record.mvrObj != null) return Result.Success(Unit)
//
//        val result = Gateway.search.fetchRecordMODS(record.id)
//        if (result is Result.Error) return result
//        val modsObj = result.get()
//        record.mvrObj = modsObj
//
//        return Result.Success(Unit)
        return Result.Error(Exception("Not implemented"))
    }

    suspend fun loadRecordAttributesAsync(record: MBRecord, id: Int = record.id): Result<Unit> {
//        if (record.attrs != null) return Result.Success(Unit)
//
//        val mraResult = Gateway.pcrud.fetchMRA(id)
//        if (mraResult is Result.Error) return mraResult
//        val mraObj = mraResult.get()
//        record.updateFromMRAResponse(mraObj)
//
//        return Result.Success(Unit)
        return Result.Error(Exception("Not implemented"))
    }

    suspend fun loadRecordMarcAsync(record: MBRecord): Result<Unit> {
//        if (record.marcRecord != null) return Result.Success(Unit)
//
//        val result = Gateway.pcrud.fetchMARC(record.id)
//        if (result is Result.Error) return result
//        val breObj = result.get()
//        record.updateFromBREResponse(breObj)
//
//        return Result.Success(Unit)
        return Result.Error(Exception("Not implemented"))
    }

    suspend fun loadRecordCopyCountAsync(record: MBRecord, orgId: Int): Result<Unit> {
        if (record.copyCounts != null) return Result.Success(Unit)

        val result = Gateway.search.fetchCopyCount(record.id, orgId)
        if (result is Result.Error) return result
        val objList = result.get()
        record.copyCounts = CopyCount.makeArray(objList)

        return Result.Success(Unit)
    }

    suspend fun loadSMSCarriersAsync(): Result<Unit> {
        if (EgSms.carriers.isNotEmpty()) return Result.Success(Unit)

        val result = Gateway.pcrud.fetchSMSCarriers()
        if (result is Result.Error) return result
        EgSms.loadCarriers(result.get())
        return Result.Success(Unit)
    }
}
