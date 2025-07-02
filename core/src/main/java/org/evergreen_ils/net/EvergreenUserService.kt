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

import net.kenstir.hemlock.data.JSONDictionary
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.model.PatronList
import net.kenstir.hemlock.net.UserService
import org.evergreen_ils.Api
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.OSRFUtils
import org.evergreen_ils.model.EvergreenAccount
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.paramListOf
import java.util.Date

class EvergreenUserService: UserService {

    override fun makeAccount(username: String, authToken: String): Account {
        return EvergreenAccount(username, authToken)
    }

    override suspend fun loadUserSession(account: Account): Result<Unit> {
        return try {
            account as? EvergreenAccount
                ?: throw IllegalArgumentException("Expected EvergreenAccount, got ${account::class.java.simpleName}")

            val sessionResponse =
                XGatewayClient.fetch(Api.AUTH, Api.AUTH_SESSION_RETRIEVE, paramListOf(account.authToken), false)
            account.loadSession(sessionResponse.payloadFirstAsObject())

            val settings = listOf("card", "settings")
            val params = paramListOf(account.authToken, account.id, settings)
            val userSettingsResponse = XGatewayClient.fetch(Api.ACTOR, Api.USER_FLESHED_RETRIEVE, params, false)
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
            val response = XGatewayClient.fetch(Api.AUTH, Api.AUTH_SESSION_DELETE, params, false)
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
            val response = XGatewayClient.fetch(Api.ACTOR, Api.CONTAINERS_BY_CLASS, params, false)
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
                val queryResult = EvergreenSearchService().fetchMulticlassQuery(query, 999, false)
                if (queryResult is Result.Error) return queryResult
                bookBag.initVisibleIdsFromQuery(queryResult.get())
            }

            // then flesh the objects
            val params = paramListOf(authToken, Api.CONTAINER_CLASS_BIBLIO, patronList.id)
            val response = XGatewayClient.fetch(Api.ACTOR, Api.CONTAINER_FLESH, params, false)
            bookBag.fleshFromObject(response.payloadFirstAsObject())
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
            val response = XGatewayClient.fetch(Api.ACTOR, Api.CLEAR_CHECKOUT_HISTORY, params, false)
            response.payloadFirstAsString()
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

    private suspend fun updatePatronSettings(account: Account, settings: JSONDictionary): String {
        val (authToken, userID) = account.getCredentialsOrThrow()
        val params = paramListOf(authToken, userID, settings)
        val response = XGatewayClient.fetch(Api.ACTOR, Api.PATRON_SETTINGS_UPDATE, params, false)
        return response.payloadFirstAsString()
    }

    companion object {
    }
}
