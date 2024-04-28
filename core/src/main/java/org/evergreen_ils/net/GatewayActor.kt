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
import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.Result
import org.evergreen_ils.data.jsonMapOf
import org.evergreen_ils.data.parseOrgStringSetting
import org.evergreen_ils.system.EgOrg
import org.opensrf.util.OSRFObject
import java.util.Date

object GatewayActor: ActorService {
    override suspend fun fetchServerVersion(): Result<String> {
        return try {
            // shouldCache=false because this result is used as a cache-busting param
            val ret = Gateway.fetchString(Api.ACTOR, Api.ILS_VERSION, arrayOf(), false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchServerCacheKey(): Result<String?> {
        return try {
            // shouldCache=false because this result is used as a cache-busting param
            val settings = listOf(Api.SETTING_HEMLOCK_CACHE_KEY)
            val args = arrayOf<Any?>(EgOrg.consortiumID, settings, Api.ANONYMOUS)
            val ret = Gateway.fetchObject(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args, false)
            val value = parseOrgStringSetting(ret, Api.SETTING_HEMLOCK_CACHE_KEY)
            Result.Success(value)
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
            val options = RequestOptions(Gateway.defaultTimeoutMs, Gateway.limitedCacheTtlSeconds)
            val ret = Gateway.fetch(Api.ACTOR, Api.ORG_TREE_RETRIEVE, arrayOf(), options) {
                it.payloadFirstAsObject()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrg(orgID: Int): Result<OSRFObject> {
        return try {
            // This request is not cached, because we use it to make sure we have the most
            // up-to-date email and phone number.
            val ret = Gateway.fetchObject(Api.ACTOR, Api.ORG_UNIT_RETRIEVE, arrayOf(Api.ANONYMOUS, orgID), false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgSettings(orgID: Int): Result<OSRFObject> {
        return try {
            val settings = mutableListOf(
                Api.SETTING_CREDIT_PAYMENTS_ALLOW,
                Api.SETTING_INFO_URL,
                Api.SETTING_REQUIRE_MONOGRAPHIC_PART,
                Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
                Api.SETTING_HEMLOCK_ERESOURCES_URL,
                Api.SETTING_HEMLOCK_EVENTS_URL,
                Api.SETTING_HEMLOCK_MEETING_ROOMS_URL,
                Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL,
            )
            if (orgID == EgOrg.consortiumID)
                settings.add(Api.SETTING_SMS_ENABLE)
            val args = arrayOf<Any?>(orgID, settings, Api.ANONYMOUS)
            val ret = Gateway.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args, true) {
                it.payloadFirstAsObject()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgHours(account: Account, orgID: Int): Result<OSRFObject?> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, orgID)
            val ret = Gateway.fetch(Api.ACTOR, Api.HOURS_OF_OPERATION_RETRIEVE, args, false) {
                it.payloadFirstAsOptionalObject()
            }
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgClosures(account: Account, orgID: Int): Result<List<OSRFObject>> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            // Neither the default start_date in ClosedDates::fetch_dates nor the start_date
            // in [param] is working to limit the results; we get all closures since day 1.
            val param = jsonMapOf("orgid" to orgID)
            val args = arrayOf<Any?>(authToken, param)
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.HOURS_CLOSED_RETRIEVE, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchOrgAddress(addressID: Int?): Result<OSRFObject?> {
        if (addressID == null)
            return Result.Success(null)
        return try {
            val args = arrayOf<Any?>(addressID)
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
            val ret = Gateway.fetchObject(Api.ACTOR, Api.USER_FLESHED_RETRIEVE, args, false)
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

    override suspend fun fetchCheckoutHistory(account: Account): Result<List<OSRFObject>> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken)
            val ret = Gateway.fetchMaybeEmptyArray(Api.ACTOR, Api.CHECKOUT_HISTORY, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** remove [circIDs] from history, or all if [circIDs] is null */
    override suspend fun clearCheckoutHistory(account: Account, circIDs: List<Int>?): Result<String> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val args: Array<Any?>
            if (circIDs != null) {
                val param = jsonMapOf("circ_ids" to circIDs)
                args = arrayOf(authToken, param)
            } else {
                args = arrayOf(authToken)
            }
            val ret = Gateway.fetchString(Api.ACTOR, Api.CLEAR_CHECKOUT_HISTORY, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun fetchMessages(account: Account): Result<List<OSRFObject>> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val args = arrayOf<Any?>(authToken, userID, null)
            val ret = Gateway.fetchObjectArray(Api.ACTOR, Api.MESSAGES_RETRIEVE, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun markMessageAction(account: Account, messageId: Int, action: String): Result<Unit> {
        return try {
            val (authToken, _) = account.getCredentialsOrThrow()
            val url = Gateway.getUrl("/eg/opac/myopac/messages?action=$action&message_id=$messageId")
            Gateway.fetchOPAC(url, authToken, RequestOptions(Gateway.defaultTimeoutMs, false, true))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markMessageDeleted(account: Account, messageId: Int): Result<Unit> =
        markMessageAction(account, messageId, "mark_deleted")

    override suspend fun markMessageRead(account: Account, messageId: Int): Result<Unit> =
        markMessageAction(account, messageId, "mark_read")

    override suspend fun markMessageUnread(account: Account, messageId: Int): Result<Unit>  =
        markMessageAction(account, messageId, "mark_unread")

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

    override suspend fun updatePatronSettings(account: Account, name: String, value: String?): Result<String> {
        return try {
            val (authToken, userID) = account.getCredentialsOrThrow()
            val param = jsonMapOf(name to value)
            val args = arrayOf<Any?>(authToken, userID, param)
            val ret = Gateway.fetchString(Api.ACTOR, Api.PATRON_SETTINGS_UPDATE, args, false)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun enableCheckoutHistory(account: Account): Result<Unit> {
        return try {
            val dateString = OSRFUtils.formatDateAsDayOnly(Date())
            val result = updatePatronSettings(account, Api.USER_SETTING_CIRC_HISTORY_START, dateString)
            when (result) {
                is Result.Success -> {
                    account.circHistoryStart = dateString
                    return Result.Success(Unit)
                }
                is Result.Error -> {
                    return result
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun disableCheckoutHistory(account: Account): Result<Unit> {
        return try {
            val result = updatePatronSettings(account, Api.USER_SETTING_CIRC_HISTORY_START, null)
            when (result) {
                is Result.Success -> {
                    account.circHistoryStart = null
                    return Result.Success(Unit)
                }
                is Result.Error -> {
                    return result
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePushNotificationToken(account: Account, token: String?): Result<Unit> {
        return try {
            //TODO: make this a real json object and store in hemlock.user_data?
            val value = token
            val result = updatePatronSettings(account, Api.USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA, value)
            when (result) {
                is Result.Success -> {
                    account.storedFcmToken = value
                    return Result.Success(Unit)
                }
                is Result.Error -> {
                    return result
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
