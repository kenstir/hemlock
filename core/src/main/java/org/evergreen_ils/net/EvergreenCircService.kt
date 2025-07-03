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
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.model.HoldRecord
import net.kenstir.hemlock.net.CircService
import net.kenstir.hemlock.net.HoldOptions
import org.evergreen_ils.Api
import org.evergreen_ils.data.EvergreenHoldRecord
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
        TODO("Not yet implemented")
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
