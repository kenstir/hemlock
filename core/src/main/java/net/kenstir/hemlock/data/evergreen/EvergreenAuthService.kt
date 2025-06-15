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
import net.kenstir.hemlock.data.jsonMapOf
import java.security.MessageDigest

class EvergreenAuthService: AuthService {
    override suspend fun fetchServerVersion(): Result<String> {
        return try {
            // shouldCache=false because this result is used as a cache-busting param
            val json = XGatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)
            val ret = XGatewayResult.create(json).payloadFirstAsString()
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun login(username: String, password: String): Result<String> {
//        return Result.Success("AuthToken12345")
        return try {
            // step 1: get seed
            val json = XGatewayClient.fetch(Api.AUTH, Api.AUTH_INIT, paramListOf(username), false)
            println("recv: $json")
            val seed = XGatewayResult.create(json).payloadFirstAsString()
            Result.Success(seed)

            // step 2: complete auth with seed + password
            val authParams = jsonMapOf(
                "type" to "persist", // {opac|persist}, controls authtoken timeout
                "username" to username,
                "password" to md5(seed + md5(password))
            )
            val json2 = XGatewayClient.fetch(Api.AUTH, Api.AUTH_COMPLETE, paramListOf(authParams), false)
            println("recv: $json2")

            // step 3: get authtoken from response
            // {"payload":[{"payload":{"authtoken":"***","authtime":1209600},"ilsevent":0,"textcode":"SUCCESS","desc":"Success"}],"status":200}
            val obj = XGatewayResult.create(json2).payloadFirstAsObject()
            val payload = obj.getObject("payload") ?: throw GatewayError("Missing payload in login response")
            val authToken = payload.getString("authtoken") ?: throw GatewayError("Missing authtoken in login response")
            //val authTime = payload.getInt("authtime") ?: throw GatewayError("Missing authtime in login  response")
            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun logout(authToken: String): Result<Unit> {
        return Result.Success(Unit)
    }

    private fun md5(s: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(s.toByteArray())
        val messageDigest = digest.digest()

        // Create Hex String
        val hexString = StringBuilder()
        for (i in messageDigest.indices) {
            val hex = Integer.toHexString(0xFF and messageDigest[i].toInt())
            if (hex.length == 1) {
                // could use a for loop, but we're only dealing with a single byte
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
