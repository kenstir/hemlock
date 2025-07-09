/*
 * Copyright (c) 2020 Kenneth H. Cox
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
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import org.opensrf.util.OSRFObject

object GatewayCirc : OldCircService {
    override suspend fun cancelHoldAsync(account: Account, holdId: Int): Result<String?> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val note = "Cancelled by mobile app"
            val args = arrayOf<Any?>(authToken, holdId, null, note)
            val ret = Gateway.fetch(Api.CIRC, Api.HOLD_CANCEL, args, false) {
                // HOLD_CANCEL returns "1" on success
                it.payloadFirstAsString()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun renewCircAsync(account: Account, targetCopy: Int): Result<OSRFObject> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = jsonMapOf(
                    "patron" to userID,
                    "copyid" to targetCopy,
                    "opac_renewal" to 1
            )
            val args = arrayOf<Any?>(authToken, param)
            val ret = Gateway.fetchObject(Api.CIRC, Api.CIRC_RENEW, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateHoldAsync(account: Account, holdId: Int, pickupLib: Int, expireTime: String?, suspendHold: Boolean, thawDate: String?): Result<String> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = jsonMapOf(
                    "id" to holdId,
                    //"email_notify" to emailNotify,
                    "pickup_lib" to pickupLib,
                    "frozen" to suspendHold,
                    //"phone_notify" to phoneNotify,
                    //"sms_notify" to smsNotify,
                    //"sms_carrier" to smsCarrierId,
                    "expire_time" to expireTime,
                    "thaw_date" to thawDate
            )
            val args = arrayOf<Any?>(authToken, null, param)
            val ret = Gateway.fetch(Api.CIRC, Api.HOLD_UPDATE, args, false) {
                // HOLD_UPDATE returns holdId as string on success
                it.payloadFirstAsString()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
