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

import net.kenstir.data.JSONDictionary
import net.kenstir.data.model.PatronMessage
import net.kenstir.data.Result
import net.kenstir.data.jsonMapOf
import net.kenstir.data.model.Account
import net.kenstir.data.model.PatronCharges
import net.kenstir.data.model.PatronList
import net.kenstir.logging.Log
import net.kenstir.data.service.UserService
import org.evergreen_ils.Api
import org.evergreen_ils.data.model.BookBag
import org.evergreen_ils.data.model.EvergreenPatronMessage
import org.evergreen_ils.data.model.FineRecord
import org.evergreen_ils.util.OSRFUtils
import org.evergreen_ils.data.model.EvergreenAccount
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.OSRFObject
import org.evergreen_ils.gateway.paramListOf
import java.util.Date

object EvergreenUserService: UserService {
    const val TAG = "UserService"

    override fun makeAccount(username: String, authToken: String): Account {
        return EvergreenAccount(username, authToken)
    }

    override suspend fun loadUserSession(account: Account): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val sessionResponse =
                GatewayClient.fetch(Api.AUTH, Api.AUTH_SESSION_RETRIEVE, paramListOf(account.authToken), false)
            account.loadSession(sessionResponse.payloadFirstAsObject())

            val settings = listOf("card", "settings")
            val params = paramListOf(account.authToken, account.id, settings)
            val userSettingsResponse = GatewayClient.fetch(Api.ACTOR, Api.USER_FLESHED_RETRIEVE, params, false)
            account.loadFleshedUserSettings(userSettingsResponse.payloadFirstAsObject())

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteSession(account: Account): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val params = paramListOf(account.authToken)
            val response = GatewayClient.fetch(Api.AUTH, Api.AUTH_SESSION_DELETE, params, false)
            // the response is the authToken; we read it but we don't need it
            response.payloadFirstAsString()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadPatronLists(account: Account): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val (authToken, userID) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, userID, Api.CONTAINER_CLASS_BIBLIO, Api.CONTAINER_BUCKET_TYPE_BOOKBAG)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINERS_BY_CLASS, params, false)
            account.loadLists(response.payloadFirstAsObjectList())
            return Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadPatronListItems(account: Account, patronList: PatronList, queryForVisibleItems: Boolean): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")
            val bookBag = patronList as? BookBag
                ?: throw IllegalArgumentException("Expected BookBag, got ${patronList::class.java.simpleName}")

            val (authToken, _) = account.getCredentialsOrThrow()

            // query first to find visible items; CONTAINER_FLESH returns the contents including
            // items that are marked deleted
            if (queryForVisibleItems) {
                val query = "container(bre,bookbag,${bookBag.id},${account.authToken})"
                val queryResult = EvergreenSearchService.fetchMulticlassQuery(query, 999, false)
                if (queryResult is Result.Error) return queryResult
                bookBag.initVisibleIdsFromQuery(queryResult.get())
            }

            // then flesh the objects
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, patronList.id)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINER_FLESH, params, false)
            bookBag.fleshFromObject(response.payloadFirstAsObject())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createPatronList(account: Account, name: String, description: String): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = OSRFObject(netClass = "cbreb", map = jsonMapOf(
                "btype" to Api.CONTAINER_BUCKET_TYPE_BOOKBAG,
                "name" to name,
                "description" to description,
                "pub" to false,
                "owner" to userID
            ))
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINER_CREATE, params, false)
            // payload contains the listId as a string, we read it but we don't need it
            val ret = response.payloadFirstAsString()
            Log.d(TAG, "[bookbag] createBag $name result $ret")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deletePatronList(account: Account, listId: Int): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val (authToken, _) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, listId)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINER_FULL_DELETE, params, false)
            // payload contains the listId as a string, we read it but we don't need it
            val ret = response.payloadFirstAsString()
            Log.d(TAG, "[bookbag] bag $listId deleteBag result $ret")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addItemToPatronList(account: Account, listId: Int, recordId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val param = OSRFObject(netClass = "cbrebi", map = jsonMapOf(
                "bucket" to listId,
                "target_biblio_record_entry" to recordId,
                "id" to null
            ))
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINER_ITEM_CREATE, params, false)
            // payload contains the itemId as a string, we read it but we don't need it
            val ret = response.payloadFirstAsString()
            Log.d(TAG, "[bookbag] bag $listId addItem $recordId result $ret")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeItemFromPatronList(account: Account, listId: Int, itemId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, itemId)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CONTAINER_ITEM_DELETE, params, false)
            // payload contains the itemId as a string, we read it but we don't need it
            val ret = response.payloadFirstAsString()
            Log.d(TAG, "[bookbag] removeItem $itemId result $ret")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePushNotificationToken(account: Account, token: String?): Result<Unit> {
        return try {
            updatePatronSettings(account, jsonMapOf(
                Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA to token,
                Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_ENABLED to true,
            ))
            account.savedPushNotificationData = token
            account.savedPushNotificationEnabled = true
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun enableCheckoutHistory(account: Account): Result<Unit>
    {
        return try {
            val dateString = OSRFUtils.formatDateAsDayOnly(Date())
            updatePatronSettings(account, jsonMapOf(Api.USER_SETTING_CIRC_HISTORY_START to dateString))
            account.circHistoryStart = dateString
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun disableCheckoutHistory(account: Account): Result<Unit> {
        return try {
            updatePatronSettings(account, jsonMapOf(Api.USER_SETTING_CIRC_HISTORY_START to null))
            account.circHistoryStart = null
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun updatePatronSettings(account: Account, settings: JSONDictionary): String {
        val (authToken, userID) = account.getCredentialsOrThrow()
        val params = paramListOf(authToken, userID, settings)
        val response = GatewayClient.fetch(Api.ACTOR, Api.PATRON_SETTINGS_UPDATE, params, false)
        return response.payloadFirstAsString()
    }

    override suspend fun clearCheckoutHistory(account: Account): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
//            if (circIDs != null) {
//                val param = jsonMapOf("circ_ids" to circIDs)
//                params = paramListOf(authToken, param)
//            } else {
//                params = paramListOf(authToken)
//            }
            val params = paramListOf(authToken)
            val response = GatewayClient.fetch(Api.ACTOR, Api.CLEAR_CHECKOUT_HISTORY, params, false)
            response.payloadFirstAsString()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchPatronMessages(account: Account): Result<List<PatronMessage>> {
        return try {
            Result.Success(fetchPatronMessagesImpl(account))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun fetchPatronMessagesImpl(account: Account): List<PatronMessage> {
        val (authToken, userID) = account.getCredentialsOrThrow()
        val params = paramListOf(authToken, userID, null)
        val response = GatewayClient.fetch(Api.ACTOR, Api.MESSAGES_RETRIEVE, params, false)
        return EvergreenPatronMessage.makeArray(response.payloadFirstAsObjectList())
    }

    override suspend fun markMessageRead(account: Account, id: Int): Result<Unit> =
        markMessageAction(account, id, "mark_read")

    override suspend fun markMessageUnread(account: Account, id: Int): Result<Unit> =
        markMessageAction(account, id, "mark_unread")

    override suspend fun markMessageDeleted(account: Account, id: Int): Result<Unit> =
        markMessageAction(account, id, "mark_deleted")

    private suspend fun markMessageAction(account: Account, messageId: Int, action: String): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val url = GatewayClient.getUrl("/eg/opac/myopac/messages?action=$action&message_id=$messageId")
            val response = GatewayClient.getOPAC(url, authToken, false)
            response.discardResponseBody()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchPatronCharges(account: Account): Result<PatronCharges> {
        return try {
            Result.Success(fetchPatronChargesImpl(account))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun fetchPatronChargesImpl(account: Account): PatronCharges {
        val (authToken, userID) = account.getCredentialsOrThrow()

        val params = paramListOf(authToken, userID)
        val summaryResponse = GatewayClient.fetch(Api.ACTOR, Api.FINES_SUMMARY, params, false)
        val chargesSummary = summaryResponse.payloadFirstAsObjectOrNull()

        if (chargesSummary == null) {
            return PatronCharges(0.0, 0.0, 0.0, emptyList())
        }

        val transactionsResponse = GatewayClient.fetch(Api.ACTOR, Api.TRANSACTIONS_WITH_CHARGES, params, false)
        val objList = transactionsResponse.payloadFirstAsObjectList()
        return PatronCharges(
            totalCharges = chargesSummary.getDouble("total_owed"),
            totalPaid = chargesSummary.getDouble("total_paid"),
            balanceOwed = chargesSummary.getDouble("balance_owed"),
            transactions = FineRecord.makeArray(objList)
        )
    }
}
