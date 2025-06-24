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

import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.JSONDictionary
import net.kenstir.hemlock.data.Result
import org.opensrf.util.OSRFObject

interface ActorService {
    suspend fun fetchOrgHours(account: Account, orgID: Int): Result<OSRFObject?>
    suspend fun fetchOrgClosures(account: Account, orgID: Int): Result<List<OSRFObject>>
    suspend fun fetchOrgAddress(addressID: Int?): Result<OSRFObject?>
    suspend fun fetchUserCheckedOut(account: Account): Result<OSRFObject>
    suspend fun fetchCheckoutHistory(account: Account): Result<List<OSRFObject>>
    suspend fun clearCheckoutHistory(account: Account, circIDs: List<Int>?): Result<String>
    suspend fun fetchMessages(account: Account): Result<List<OSRFObject>>
    suspend fun markMessageDeleted(account: Account, messageId: Int): Result<Unit>
    suspend fun markMessageRead(account: Account, messageId: Int): Result<Unit>
    suspend fun markMessageUnread(account: Account, messageId: Int): Result<Unit>
    suspend fun fetchUserFinesSummary(account: Account): Result<OSRFObject?>
    suspend fun fetchUserTransactionsWithCharges(account: Account): Result<List<OSRFObject>>
    suspend fun fetchBookBags(account: Account): Result<List<OSRFObject>>
    suspend fun fleshBookBagAsync(account: Account, bookBagId: Int): Result<OSRFObject>
    suspend fun createBookBagAsync(account: Account, name: String): Result<Unit>
    suspend fun deleteBookBagAsync(account: Account, bookBagId: Int): Result<Unit>
    suspend fun addItemToBookBagAsync(account: Account, bookBagId: Int, recordId: Int): Result<Unit>
    suspend fun removeItemFromBookBagAsync(account: Account, bookBagItemId: Int): Result<Unit>
    suspend fun updatePatronSettings(account: Account, settings: JSONDictionary): Result<String>
    suspend fun enableCheckoutHistory(account: Account): Result<Unit>
    suspend fun disableCheckoutHistory(account: Account): Result<Unit>
    suspend fun updatePushNotificationToken(account: Account, token: String?): Result<Unit>
}
