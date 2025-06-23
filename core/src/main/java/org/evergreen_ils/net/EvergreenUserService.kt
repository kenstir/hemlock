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
import org.evergreen_ils.datax.XGatewayClient
import org.evergreen_ils.datax.paramListOf
import org.evergreen_ils.datax.payloadFirstAsObject
import net.kenstir.hemlock.net.UserService
import net.kenstir.hemlock.data.model.Account
import org.evergreen_ils.Api
import org.evergreen_ils.model.EvergreenAccount

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
        TODO("Not yet implemented")
    }
}
