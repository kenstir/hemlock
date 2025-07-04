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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.ShouldNotHappenException
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.model.HoldRecord
import net.kenstir.hemlock.net.CircService
import net.kenstir.hemlock.net.HoldOptions
import org.evergreen_ils.Api
import org.evergreen_ils.HOLD_TYPE_COPY
import org.evergreen_ils.HOLD_TYPE_FORCE
import org.evergreen_ils.HOLD_TYPE_METARECORD
import org.evergreen_ils.HOLD_TYPE_PART
import org.evergreen_ils.HOLD_TYPE_RECALL
import org.evergreen_ils.HOLD_TYPE_TITLE
import org.evergreen_ils.HOLD_TYPE_VOLUME
import org.evergreen_ils.data.EvergreenHoldRecord
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.paramListOf

class EvergreenCircService: CircService {
    override suspend fun fetchHolds(account: Account): Result<List<HoldRecord>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, userID)
            val response = XGatewayClient.fetch(Api.CIRC, Api.HOLDS_RETRIEVE, params, false)
            Result.Success(EvergreenHoldRecord.makeArray(response.payloadFirstAsObjectList()))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadHoldDetails(account: Account, holdRecord: HoldRecord): Result<Unit> {
        if (holdRecord !is EvergreenHoldRecord) {
            return Result.Error(IllegalArgumentException("Expected EvergreenHoldRecord, got ${holdRecord::class.java.name}"))
        }
        return try {
            Result.Success(loadHoldDetailsImpl(account, holdRecord))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadHoldDetailsImpl(account: Account, hold: EvergreenHoldRecord): Unit = coroutineScope {
        val target = hold.target ?: throw GatewayError("null hold target")
        val jobs = mutableListOf<Deferred<Any>>()
        jobs.add(async {
            when (hold.holdType) {
                HOLD_TYPE_TITLE ->
                    loadTitleHoldTargetDetails(account, hold, target)
                HOLD_TYPE_METARECORD ->
                    //loadMetarecordHoldTargetDetails(account, hold, target)
                    Unit
                HOLD_TYPE_PART ->
                    //loadPartHoldTargetDetails(account, hold, target)
                    Unit
                HOLD_TYPE_COPY, HOLD_TYPE_FORCE, HOLD_TYPE_RECALL ->
                    //loadCopyHoldTargetDetails(account, hold, target)
                    Unit
                HOLD_TYPE_VOLUME ->
                    //loadVolumeHoldTargetDetails(account, hold, target)
                    Unit
                else -> {
                    Analytics.logException(ShouldNotHappenException("unexpected holdType:${hold.holdType}"))
                    Result.Error(GatewayError("unexpected hold type: ${hold.holdType}"))
                }
            }
        })
        jobs.add(async {
            loadHoldQueueStats(account, hold)
        })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
    }

    private suspend fun loadHoldQueueStats(account: Account, hold: EvergreenHoldRecord) {
        val id = hold.id
        val (authToken, userID) = account.getCredentialsOrThrow()
        val params = paramListOf(authToken, hold.id)
        val response = XGatewayClient.fetch(Api.CIRC, Api.HOLD_QUEUE_STATS, params, false)
        hold.qstatsObj = response.payloadFirstAsObject()
    }

    private suspend fun loadTitleHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        val modsObj = EvergreenBiblioService.fetchRecordMODS(target)
        val bibRecord = MBRecord(target, modsObj)
        hold.record = bibRecord

        val attrsObj = EvergreenBiblioService.fetchMRA(target)
        bibRecord?.updateFromMRAResponse(attrsObj)
    }

    override suspend fun placeHold(account: Account, targetId: Int, options: HoldOptions): Result<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun updateHold(account: Account, holdId: Int, options: HoldOptions): Result<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelHold(account: Account, holdId: String): Result<Unit> {
        TODO("Not yet implemented")
    }
}
