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

import net.kenstir.data.service.AuthService
import net.kenstir.data.Result
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.paramListOf
import net.kenstir.data.jsonMapOf
import net.kenstir.util.md5
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.GatewayException

object EvergreenAuthService: AuthService {

    override suspend fun getAuthToken(username: String, password: String): Result<String> {
        return try {
            // step 1: get seed
            val initResponse = GatewayClient.fetch(Api.AUTH, Api.AUTH_INIT, paramListOf(username), false)
            val seed = initResponse.payloadFirstAsString()

            // step 2: complete auth with seed + password
            val options = jsonMapOf(
                "type" to "persist", // {opac|persist}, controls authtoken timeout
                "username" to username,
                "password" to md5(seed + md5(password))
            )
            val response = GatewayClient.fetch(Api.AUTH, Api.AUTH_COMPLETE, paramListOf(options), false)
            val obj = response.payloadFirstAsObject()

            // step 3: get authtoken from response
            // {"payload":[{"payload":{"authtoken":"***","authtime":1209600},"ilsevent":0,"textcode":"SUCCESS","desc":"Success"}],"status":200}
            val payload = obj.getObject("payload") ?: throw GatewayException("Missing payload in login response")
            val authToken = payload.getString("authtoken") ?: throw GatewayException("Missing authtoken in login response")
            //val authTime = payload.getInt("authtime") ?: throw GatewayError("Missing authtime in login  response")
            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun md5(input: String): String {
        return input.md5()
    }
}
