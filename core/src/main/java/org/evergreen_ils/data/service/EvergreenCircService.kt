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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kenstir.util.Analytics
import net.kenstir.data.MutableJSONDictionary
import net.kenstir.data.Result
import net.kenstir.data.ShouldNotHappenException
import net.kenstir.data.jsonMapOf
import net.kenstir.data.model.Account
import net.kenstir.data.model.CircRecord
import net.kenstir.data.model.HistoryRecord
import net.kenstir.data.model.HoldPart
import net.kenstir.data.model.HoldRecord
import net.kenstir.data.service.CircService
import net.kenstir.data.service.HoldOptions
import net.kenstir.data.service.HoldUpdateOptions
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.GatewayError
import org.evergreen_ils.data.model.EvergreenCircRecord
import org.evergreen_ils.data.model.EvergreenHistoryRecord
import org.evergreen_ils.data.model.EvergreenHoldRecord
import org.evergreen_ils.data.model.MBRecord
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.gateway.paramListOf

object EvergreenCircService: CircService {
    override suspend fun fetchCheckouts(account: Account): Result<List<CircRecord>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, userID)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CHECKED_OUT, params, false)
            Result.Success(EvergreenCircRecord.makeArray(response.payloadFirstAsObject()))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadCheckoutDetails(account: Account, circRecord: CircRecord): Result<Unit> {
        if (circRecord !is EvergreenCircRecord) return Result.Error(IllegalArgumentException("Expected EvergreenCircRecord, got ${circRecord::class.java.name}"))

        return try {
            val circObj = fetchCircRecord(account,  circRecord.circId)
            circRecord.circ = circObj

            val targetCopy = circObj.getInt("target_copy") ?: throw GatewayError("circ item has no target_copy")
            val modsObj = EvergreenBiblioService.fetchCopyMODS(targetCopy)
            val record = MBRecord(modsObj)
            circRecord.record = record

            if (record.isPreCat) {
                val acpObj = fetchAssetCopy(targetCopy)
                circRecord.acp = acpObj
            } else {
                val mraObj = EvergreenBiblioService.fetchMRA(record.id)
                record.updateFromMRAResponse(mraObj)
            }

            return Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun fetchCircRecord(account: Account, circId: Int): OSRFObject {
        val (authToken, _) = account.getCredentialsOrThrow()
        val params = paramListOf(authToken, circId)
        val response = GatewayClient.fetch(Api.CIRC, Api.CIRC_RETRIEVE, params, false)
        return response.payloadFirstAsObject()
    }

    override suspend fun renewCheckout(account: Account, targetCopy: Int): Result<Boolean> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = jsonMapOf(
                "patron" to userID,
                "copyid" to targetCopy,
                "opac_renewal" to 1
            )
            val params = paramListOf(authToken, param)
            val response = GatewayClient.fetch(Api.CIRC, Api.CIRC_RENEW, params, false)
            // CIRC_RENEW returns a JSON object, but as long as it's not a failure event, the renewal succeeded.
            val obj = response.payloadFirstAsObject()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchCheckoutHistory(account: Account): Result<List<HistoryRecord>> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CHECKOUT_HISTORY, params, false)
            Result.Success(EvergreenHistoryRecord.makeArray(response.payloadAsObjectList()))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadHistoryDetails(historyRecord: HistoryRecord): Result<Unit> {
        historyRecord as? EvergreenHistoryRecord ?: return Result.Error(IllegalArgumentException("Expected EvergreenHistoryRecord, got ${historyRecord::class.java.name}"))

        if (historyRecord.record != null) {
            // already loaded
            return Result.Success(Unit)
        }

        return try {
            val targetCopy = historyRecord.targetCopy ?: throw GatewayError("circ item has no target_copy")
            val modsObj = EvergreenBiblioService.fetchCopyMODS(targetCopy)
            val record = MBRecord(modsObj)
            historyRecord.record = record
//
//            val mraObj = EvergreenBiblioService.fetchMRA(record.id)
//            record.updateFromMRAResponse(mraObj)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchHolds(account: Account): Result<List<HoldRecord>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, userID)
            val response = GatewayClient.fetch(Api.CIRC, Api.HOLDS_RETRIEVE, params, false)
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
                Api.HoldType.TITLE ->
                    loadTitleHoldTargetDetails(account, hold, target)
                Api.HoldType.METARECORD ->
                    loadMetarecordHoldTargetDetails(account, hold, target)
                Api.HoldType.PART ->
                    loadPartHoldTargetDetails(account, hold, target)
                Api.HoldType.COPY, Api.HoldType.FORCE, Api.HoldType.RECALL ->
                    loadCopyHoldTargetDetails(account, hold, target)
                Api.HoldType.VOLUME ->
                    loadVolumeHoldTargetDetails(account, hold, target)
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
        val response = GatewayClient.fetch(Api.CIRC, Api.HOLD_QUEUE_STATS, params, false)
        hold.qstatsObj = response.payloadFirstAsObject()
    }

    private suspend fun loadTitleHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        val modsObj = EvergreenBiblioService.fetchRecordMODS(target)
        val bibRecord = MBRecord(target, modsObj)
        hold.record = bibRecord

        val attrsObj = EvergreenBiblioService.fetchMRA(target)
        bibRecord?.updateFromMRAResponse(attrsObj)
    }

    private suspend fun loadMetarecordHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        val mvrObj = EvergreenBiblioService.fetchMetarecordMODS(target)
        val bibRecord = MBRecord(mvrObj.getInt("tcn") ?: -1, mvrObj)
        hold.record = bibRecord
    }

    private suspend fun loadPartHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        val bmpObj = fetchBMP(target)
        val id = bmpObj.getInt("record") ?: throw GatewayError("missing record number in part hold bre")
        hold.partLabel = bmpObj.getString("label")

        val modsObj = EvergreenBiblioService.fetchRecordMODS(id)
        val bibRecord = MBRecord(modsObj)
        hold.record = bibRecord

        val mraObj = EvergreenBiblioService.fetchMRA(bibRecord.id)
        bibRecord.updateFromMRAResponse(mraObj)
    }

    private suspend fun fetchBMP(holdTarget: Int): OSRFObject {
        val param = jsonMapOf(
            "cache" to 1,
            "fields" to arrayListOf("label", "record"),
            "query" to jsonMapOf("id" to holdTarget)
        )
        val response = GatewayClient.fetch(Api.FIELDER, Api.FIELDER_BMP_ATOMIC, paramListOf(param), false)
        val list = response.payloadFirstAsObjectList()
        return list.first()
    }

    private suspend fun loadCopyHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        // steps: hold target -> asset copy -> asset.call_number -> mods

        val acpObj = fetchAssetCopy(target)
        val callNumber = acpObj.getInt("call_number") ?: throw GatewayError("missing call_number in copy hold")

        val acnObj = fetchAssetCallNumber(callNumber)
        val id = acnObj.getInt("record") ?: throw GatewayError("missing record number in asset call number")

        val modsObj = EvergreenBiblioService.fetchRecordMODS(id)
        val bibRecord = MBRecord(modsObj)
        hold.record = bibRecord

        val mraObj = EvergreenBiblioService.fetchMRA(bibRecord.id)
        bibRecord.updateFromMRAResponse(mraObj)
    }

    private suspend fun fetchAssetCopy(copyId: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.SEARCH, Api.ASSET_COPY_RETRIEVE, paramListOf(copyId), true)
        return response.payloadFirstAsObject()
    }

    private suspend fun fetchAssetCallNumber(callNumber: Int): OSRFObject {
        val response = GatewayClient.fetch(Api.SEARCH, Api.ASSET_CALL_NUMBER_RETRIEVE, paramListOf(callNumber), true)
        return response.payloadFirstAsObject()
    }

    private suspend fun loadVolumeHoldTargetDetails(account: Account, hold: EvergreenHoldRecord, target: Int) {
        // steps: hold target -> asset call number -> mods

        val acnObj = fetchAssetCallNumber(target)
        val id = acnObj.getInt("record") ?: throw GatewayError("missing record number in asset call number")

        val modsObj = EvergreenBiblioService.fetchRecordMODS(id)
        val bibRecord = MBRecord(modsObj)
        hold.record = bibRecord

        val mraObj = EvergreenBiblioService.fetchMRA(bibRecord.id)
        bibRecord.updateFromMRAResponse(mraObj)
    }

    override suspend fun fetchHoldParts(targetId: Int): Result<List<HoldPart>> {
        return try {
            val query = jsonMapOf("record" to targetId)
            val response = GatewayClient.fetch(Api.SEARCH,Api.HOLD_PARTS, paramListOf(query), true)
            val parts = response.payloadFirstAsObjectList()
            Result.Success(parts.map { HoldPart(it.getInt("id") ?: -1, it.getString("label") ?: "Unknown part") })
        } catch (e: Exception) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchTitleHoldIsPossible(account: Account, targetId: Int, pickupLib: Int): Result<Boolean> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = jsonMapOf(
                "patronid" to userID,
                "pickup_lib" to pickupLib,
                "hold_type" to Api.HoldType.TITLE,
                "titleid" to targetId
            )
            val params = paramListOf(authToken, param)
            val response = GatewayClient.fetch(Api.CIRC, Api.TITLE_HOLD_IS_POSSIBLE, params, false)
            // The response is a JSON object with details, e.g. "success":1.  But if a title hold is not posssible,
            // the response includes an event and an error is thrown while deserializing.
            response.payloadFirstAsObject()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun placeHold(account: Account, targetId: Int, options: HoldOptions): Result<Boolean> {
        return try {
            Result.Success(placeHoldImpl(account, targetId, options))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun placeHoldImpl(account: Account, targetId: Int, options: HoldOptions): Boolean {
        val (authToken, userID) = account.getCredentialsOrThrow()
        val param: MutableJSONDictionary = mutableMapOf(
            "patronid" to userID,
            "pickup_lib" to options.pickupLib,
            "hold_type" to options.holdType,
            "email_notify" to options.emailNotify,
            "expire_time" to options.expireTime,
            "frozen" to options.suspendHold
        )
        if (!options.phoneNotify.isNullOrEmpty()) {
            param["phone_notify"] = options.phoneNotify
        }
        if (options.smsCarrierId != null && !options.smsNotify.isNullOrEmpty()) {
            param["sms_carrier"] = options.smsCarrierId
            param["sms_notify"] = options.smsNotify
        }
        if (!options.thawDate.isNullOrEmpty()) {
            param["thaw_date"] = options.thawDate
        }

        val params = paramListOf(authToken, param, arrayListOf(targetId))
        val method = if (options.useOverride) Api.HOLD_TEST_AND_CREATE_OVERRIDE else Api.HOLD_TEST_AND_CREATE
        val response = GatewayClient.fetch(Api.CIRC, method, params, false)
        val obj = response.payloadFirstAsObject()
        return true
    }

    override suspend fun updateHold(account: Account, holdId: Int, options: HoldUpdateOptions): Result<Boolean> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val param = jsonMapOf(
                "id" to holdId,
                "pickup_lib" to options.pickupLib,
                "frozen" to options.suspendHold,
                "expire_time" to options.expireTime,
                "thaw_date" to options.thawDate,
            )

            val params = paramListOf(authToken, null, param)
            val response = GatewayClient.fetch(Api.CIRC, Api.HOLD_UPDATE, params, false)
            // HOLD_UPDATE returns holdId as string on success
            val str = response.payloadFirstAsString()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelHold(account: Account, holdId: Int): Result<Boolean> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val note = "Cancelled by mobile app"
            val params = paramListOf(authToken, holdId, null, note)
            val response = GatewayClient.fetch(Api.CIRC, Api.HOLD_CANCEL, params, false)
            // HOLD_CANCEL returns "1" on success, and an error event if it fails.
            val str = response.payloadFirstAsString()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
