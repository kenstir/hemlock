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
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import org.opensrf.util.OSRFObject

object GatewayActor: ActorService {

    override suspend fun createBookBagAsync(account: Account, name: String): Result<Unit> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = OSRFObject("cbreb", jsonMapOf(
                    "btype" to Api.CONTAINER_BUCKET_TYPE_BOOKBAG,
                    "name" to name,
                    "pub" to false,
                    "owner" to userID
            ))
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_CREATE, args, false) {
                // e.g. "9"
                Log.d(TAG, "[bookbag] createBag $name result ${it.payload}")
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
                Log.d(TAG, "[bookbag] bag $bookBagId deleteBag result ${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addItemToBookBagAsync(account: Account, bookBagId: Int, recordId: Int): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val param = OSRFObject("cbrebi", jsonMapOf(
                    "bucket" to bookBagId,
                    "target_biblio_record_entry" to recordId,
                    "id" to null
            ))
            val args = arrayOf<Any?>(authToken, Api.CONTAINER_CLASS_BIBLIO, param)
            val ret = Gateway.fetch(Api.ACTOR, Api.CONTAINER_ITEM_CREATE, args, false) {
                // e.g. "579683692"
                Log.d(TAG, "[bookbag] bag $bookBagId addItem $recordId result ${it.payload}")
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
                // e.g. "498474680"
                Log.d(TAG, "[bookbag] removeItem $bookBagItemId result ${it.payload}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
