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
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.JSONDictionary
import org.evergreen_ils.data.Result
import org.evergreen_ils.data.jsonMapOf
import org.opensrf.util.OSRFObject

object GatewayActor: ActorService {
    override suspend fun fetchServerVersion(): Result<String> {
        return try {
            val ret = Gateway.fetchObjectString(Api.ACTOR, Api.ILS_VERSION, arrayOf(), false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgTypes(): Result<List<OSRFObject>> {
        return try {
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, arrayOf(), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgTree(): Result<OSRFObject> {
        return try {
            val ret = Gateway.fetchObject(Api.ACTOR, Api.ORG_TREE_RETRIEVE, arrayOf(), true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgSettings(orgID: Int): Result<OSRFObject> {
        return try {
            val settings = arrayListOf(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
                    Api.SETTING_CREDIT_PAYMENTS_ALLOW, Api.SETTING_INFO_URL)
            val args = arrayOf<Any?>(orgID, settings, Api.ANONYMOUS)
            val ret = Gateway.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args, true) {
                it.asObject()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgHours(account: Account, orgID: Int?): Result<OSRFObject> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, orgID)
            val ret = Gateway.fetch(Api.ACTOR, Api.HOURS_OF_OPERATION_RETRIEVE, args, true) {
                it.asObject()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgAddress(orgID: Int?): Result<OSRFObject> {
        return try {
            val args = arrayOf<Any?>(orgID)
            val ret = Gateway.fetchObject(Api.ACTOR, Api.ADDRESS_RETRIEVE, args, true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchFleshedUser(account: Account): Result<OSRFObject> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val settings = listOf("card", "settings")
            val args = arrayOf<Any?>(authToken, userID, settings)
            val ret = Gateway.fetchObject(Api.ACTOR, Api.USER_FLESHED_RETRIEVE, args, true)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchUserCheckedOut(account: Account): Result<OSRFObject> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID)
            val ret = Gateway.fetchObject(Api.ACTOR, Api.CHECKED_OUT, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchUserMessages(account: Account): Result<List<OSRFObject>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID, null)
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.MESSAGES_RETRIEVE, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchUserFinesSummary(account: Account): Result<OSRFObject?> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID)
            val ret = Gateway.fetchOptionalObject(Api.ACTOR, Api.FINES_SUMMARY, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchUserTransactionsWithCharges(account: Account): Result<List<OSRFObject>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID)
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.TRANSACTIONS_WITH_CHARGES, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchBookBags(account: Account): Result<List<OSRFObject>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID, Api.CONTAINER_CLASS_BIBLIO, Api.CONTAINER_BUCKET_TYPE_BOOKBAG)
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.CONTAINERS_BY_CLASS, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fleshBookBagAsync(account: Account, bookBagId: Int): Result<OSRFObject> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, bookBagId)
            val ret = Gateway.fetchObject(Api.ACTOR, Api.CONTAINER_FLESH, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createBookBagAsync(account: Account, name: String): Result<Unit> {
        return try {
            val (authToken, userId) = account.getCredentialsOrThrow()
            var param = OSRFObject("cbreb", jsonMapOf(
                    "btype" to Api.CONTAINER_BUCKET_TYPE_BOOKBAG,
                    "name" to name,
                    "pub" to false,
                    "owner" to userId
            ))
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_CREATE, args, false) {
                // e.g. "9"
                Log.d(TAG, "[kcxxx] createResult:${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteBookBagAsync(account: Account, bookBagId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, bookBagId)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_FULL_DELETE, args, false) {
                // e.g. "9"
                Log.d(TAG, "[kcxxx] deleteResult:${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addItemToBookBagAsync(account: Account, bookBagId: Int, recordId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            var param = jsonMapOf(
                    "bucket" to bookBagId,
                    "target_biblio_record_entry" to recordId,
                    "id" to null
            )
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_ITEM_CREATE, args, false) {
                // e.g. ???
                Log.d(TAG, "[kcxxx] addItemResult:${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeItemFromBookBagAsync(account: Account, bookBagItemId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, bookBagItemId)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_ITEM_DELETE, args, false) {
                // e.g. ???
                Log.d(TAG, "[kcxxx] removeItemResult:${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
