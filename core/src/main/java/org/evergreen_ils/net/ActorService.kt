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

import org.evergreen_ils.data.Account
import org.evergreen_ils.data.JSONDictionary
import org.evergreen_ils.data.Result
import org.opensrf.util.OSRFObject

interface ActorService {
    suspend fun fetchServerVersion(): Result<String>
    suspend fun fetchOrgTypes(): Result<List<OSRFObject>>
    suspend fun fetchOrgTree(): Result<OSRFObject>
    suspend fun fetchOrgSettings(orgID: Int): Result<JSONDictionary>
    suspend fun fetchFleshedUser(account: Account): Result<OSRFObject>
    suspend fun fetchUserCheckedOut(account: Account): Result<OSRFObject>
    suspend fun fetchUserMessages(account: Account): Result<List<OSRFObject>>
    suspend fun fetchUserFinesSummary(account: Account): Result<OSRFObject?>
    suspend fun fetchUserTransactionsWithCharges(account: Account): Result<List<OSRFObject>>
    suspend fun fetchBookBags(account: Account): Result<List<OSRFObject>>
    suspend fun fleshBookBagAsync(account: Account, bookBagId: Int): Result<OSRFObject>
    suspend fun createBookBagAsync(account: Account, name: String): Result<Unit>
    suspend fun deleteBookBagAsync(account: Account, bookBagId: Int): Result<Unit>
    suspend fun addItemToBookBagAsync(account: Account, bookBagId: Int, recordId: Int): Result<Unit>
    suspend fun removeItemFromBookBagAsync(account: Account, bookBagItemId: Int): Result<Unit>
}
