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
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Organization
import org.evergreen_ils.data.Result
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.system.EgOrg

object GatewayLoader {

    // usage: loadOrgSettingsAsync(...).await()
    suspend fun loadOrgSettingsAsync(org: Organization?) = GlobalScope.async {
        val orgs = if (org != null) listOf(org) else EgOrg.visibleOrgs
        for (org in orgs) {
            if (!org.settingsLoaded) {
                async {
                    val result = Gateway.actor.fetchOrgSettings(org.id)
                    if (result is Result.Success) {
                        org.loadSettings(result.data)
                        Log.d(TAG, "[kcxxx] org ${org.id} settings loaded")
                    }
                }
            }
        }
    }

    suspend fun loadOrgAsync(org: Organization?) = GlobalScope.async {
        val id = org?.id ?: return@async
        val result = Gateway.actor.fetchOrg(id)
        if (result is Result.Success) {
            org.aouObj = result.data
            Log.d(TAG, "[kcxxx] org ${org.id} aou loaded")
        }
    }

    suspend fun loadBookBagsAsync(account: Account): Result<Unit> {
        // do not cache bookbags
//        return if (account.bookBagsLoaded) {
//            Log.d(TAG, "[bookbag] loadBookBagsAsync...noop")
//            Result.Success(Unit)
//        } else {
            Log.d(TAG, "[bookbag] loadBookBagsAsync...")
            return when (val result = Gateway.actor.fetchBookBags(account)) {
                is Result.Success -> {
                    App.getAccount().loadBookBags(result.data)
                    Log.d(TAG, "[bookbag] loadBookBagsAsync...done")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    result
                }
            }
//        }
    }

    suspend fun loadBookBagContents(account: Account, bookBag: BookBag): Result<Unit> {
        // do not cache bookbag contents
        Log.d(TAG, "[bookbag] bag:${bookBag.name}")
        val result = Gateway.actor.fleshBookBagAsync(account, bookBag.id)
        if (result is Result.Error) return result
        val obj = result.get()
        Log.d(TAG, "[bookbag] bag content:$obj")
        bookBag.fleshFromObject(obj)
        Analytics.logEvent(Analytics.Event.BOOKBAG_LOAD, bundleOf(
            Analytics.Param.NUM_ITEMS to bookBag.items.size
        ))

        return Result.Success(Unit)
    }

    suspend fun loadRecordMetadataAsync(record: RecordInfo): Result<Unit> {
        if (record.hasMetadata) return Result.Success(Unit)

        val result = Gateway.search.fetchRecordMODS(record.doc_id)
        if (result is Result.Error) return result
        val modsObj = result.get()
        RecordInfo.updateFromMODSResponse(record, modsObj)

        return Result.Success(Unit)
    }

    suspend fun loadRecordAttributesAsync(record: RecordInfo, id: Int = record.doc_id): Result<Unit> {
        if (record.hasAttributes) return Result.Success(Unit)

        val mraResult = Gateway.pcrud.fetchMRA(id)
        if (mraResult is Result.Error) return mraResult
        val mraObj = mraResult.get()
        record.updateFromMRAResponse(mraObj)

        return Result.Success(Unit)
    }

    suspend fun loadRecordMarcAsync(record: RecordInfo): Result<Unit> {
        if (record.hasMARC) return Result.Success(Unit)

        val result = Gateway.pcrud.fetchMARC(record.doc_id)
        if (result is Result.Error) return result
        val obj = result.get()
        record.updateFromBREResponse(obj)

        return Result.Success(Unit)
    }

    suspend fun loadRecordCopyCountsAsync(record: RecordInfo, orgId: Int): Result<Unit> {
        if (record.hasCopySummary) return Result.Success(Unit)

        val result = Gateway.search.fetchCopyCount(record.doc_id, orgId)
        if (result is Result.Error) return result
        val objList = result.get()
        record.updateFromCopyCountResponse(objList)

        return Result.Success(Unit)
    }
}
