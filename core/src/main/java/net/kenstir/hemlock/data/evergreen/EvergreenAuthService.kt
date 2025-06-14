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

package net.kenstir.hemlock.data.evergreen

import net.kenstir.hemlock.data.AuthService
import net.kenstir.hemlock.data.Result

class EvergreenAuthService: AuthService {
    override suspend fun fetchServerVersion(): Result<String> {
        return try {
            val json = XGatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, arrayOf(), false)
            val ret = XGatewayResult.create(json).payloadFirstAsString()
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun login(username: String, password: String): Result<String> {
        return Result.Success("AuthToken12345")
    }

    override suspend fun logout(authToken: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
