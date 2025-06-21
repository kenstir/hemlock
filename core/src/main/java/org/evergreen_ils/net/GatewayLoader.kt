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

import androidx.core.os.bundleOf
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.models.Account
import org.evergreen_ils.data.*
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSms

object GatewayLoader {

    // usage: loadOrgSettingsAsync(...).await()
    // TODO: consider passing activity:BaseActivity param and using activity.scope
    // or returning [Deferred<Result>]
    // because these org ops are happening on thread DefaultDispatcher-worker-1 :/
    suspend fun loadOrgSettingsAsync(org: Organization?) = GlobalScope.async {
        val orgs = if (org != null) listOf(org) else EgOrg.visibleOrgs
        for (org in orgs) {
            if (!org.settingsLoaded) {
                async {
                    val result = Gateway.actor.fetchOrgSettings(org.id)
                    if (result is Result.Success) {
                        //org.loadSettings(result.data)
                        // TODO: fixme
                        Log.d(TAG, "[kcxxx] org ${org.id} settings loaded")
                        org.requireMonographicPart?.let {
                            Log.d(TAG, "[kcxxx] org ${org.id} requireMonographicPart=$it")
                        }
                    }
                }
            }
        }
    }

    // this function reloads the org, so we can be sure the hours of operation are up-to-date
    // TODO: consider passing activity:BaseActivity param and using activity.scope
    // because these org ops are happening on thread DefaultDispatcher-worker-1 :/
    suspend fun loadOrgAsync(org: Organization?) = GlobalScope.async {
        val id = org?.id ?: return@async
        val result = Gateway.actor.fetchOrg(id)
        if (result is Result.Success) {
            // TODO: fixme
//            org.aouObj = result.data
            Log.v(TAG, "org id:${org.id} level:${org.level} vis:${org.opacVisible} shortname:${org.shortname} name:${org.name}")
            //Log.v(TAG, "org ${org.id} aou loaded")
        }
    }

    suspend fun loadBookBagsAsync(account: Account): Result<Unit> {
        // do not cache bookbags
        Log.d(TAG, "[bookbag] loadBookBagsAsync...")
        val result = Gateway.actor.fetchBookBags(account)
        return when (result) {
            is Result.Error -> {
                Log.d(TAG, "[bookbag] loadBookBagsAsync...error")
                result
            }
            is Result.Success -> {
                App.getAccount().loadBookBags(result.data)
                Log.d(TAG, "[bookbag] loadBookBagsAsync...${App.getAccount().bookBags.size} bags")
                Result.Success(Unit)
            }
        }
    }

    suspend fun loadBookBagContents(account: Account, bookBag: BookBag, queryForVisibleItems: Boolean): Result<Unit> {
        // do not cache bookbag contents
        Log.d(TAG, "[bookbag] bag ${bookBag.id} name ${bookBag.name}")

        // query first to find visible items; CONTAINER_FLESH returns the contents including
        // items that are marked deleted
        if (queryForVisibleItems) {
            val query = "container(bre,bookbag,${bookBag.id},${account.authToken})"
            val queryResult = Gateway.search.fetchMulticlassQuery(query, 999)
            if (queryResult is Result.Error) return queryResult
            bookBag.initVisibleIdsFromQuery(queryResult.get())
            Log.d(TAG, "[bookbag] bag ${bookBag.id} has ${bookBag.visibleRecordIds.size} visible items")
        }

        // then flesh the objects
        val result = Gateway.actor.fleshBookBagAsync(account, bookBag.id)
        if (result is Result.Error) return result
        val obj = result.get()
        bookBag.fleshFromObject(obj)
        Log.d(TAG, "[bookbag] bag ${bookBag.id} has ${bookBag.items.size} items")
        Analytics.logEvent(Analytics.Event.BOOKBAG_LOAD, bundleOf(
            Analytics.Param.NUM_ITEMS to bookBag.items.size
        ))

        return Result.Success(Unit)
    }

    suspend fun loadRecordMetadataAsync(record: MBRecord): Result<Unit> {
        if (record.mvrObj != null) return Result.Success(Unit)

        val result = Gateway.search.fetchRecordMODS(record.id)
        if (result is Result.Error) return result
        val modsObj = result.get()
        record.mvrObj = modsObj

        return Result.Success(Unit)
    }

    suspend fun loadRecordAttributesAsync(record: MBRecord, id: Int = record.id): Result<Unit> {
        if (record.attrs != null) return Result.Success(Unit)

        val mraResult = Gateway.pcrud.fetchMRA(id)
        if (mraResult is Result.Error) return mraResult
        val mraObj = mraResult.get()
        record.updateFromMRAResponse(mraObj)

        return Result.Success(Unit)
    }

    suspend fun loadRecordMarcAsync(record: MBRecord): Result<Unit> {
        if (record.marcRecord != null) return Result.Success(Unit)

        val result = Gateway.pcrud.fetchMARC(record.id)
        if (result is Result.Error) return result
        val breObj = result.get()
        record.updateFromBREResponse(breObj)

        return Result.Success(Unit)
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
