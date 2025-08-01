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

package net.kenstir.data.service

import net.kenstir.data.model.PatronMessage
import net.kenstir.data.Result
import net.kenstir.data.model.Account
import net.kenstir.data.model.PatronCharges
import net.kenstir.data.model.PatronList

interface UserService {
    fun makeAccount(username: String, authToken: String): Account
    suspend fun loadUserSession(account: Account): Result<Unit>
    suspend fun deleteSession(account: Account): Result<Unit>

    /**
     * loads the patron lists for the account into [Account.patronLists]
     *
     * NB: it does not load the items in the lists, see [loadPatronListItems]
     */
    suspend fun loadPatronLists(account: Account): Result<Unit>

    /**
     * loads the items in a patron list, optionally filtering to only those items that are visible
     *
     * NB: it does not load the records for the items, see [BiblioService.loadRecordDetails]
     */
    suspend fun loadPatronListItems(account: Account, patronList: PatronList, queryForVisibleItems: Boolean): Result<Unit>

    suspend fun createPatronList(account: Account, name: String, description: String): Result<Unit>
    suspend fun deletePatronList(account: Account, listId: Int): Result<Unit>
    suspend fun addItemToPatronList(account: Account, listId: Int, recordId: Int): Result<Unit>
    suspend fun removeItemFromPatronList(account: Account, listId: Int, itemId: Int): Result<Unit>

    suspend fun updatePushNotificationToken(account: Account, token: String?): Result<Unit>
    suspend fun enableCheckoutHistory(account: Account): Result<Unit>
    suspend fun disableCheckoutHistory(account: Account): Result<Unit>
    suspend fun clearCheckoutHistory(account: Account): Result<Unit>

    suspend fun fetchPatronMessages(account: Account): Result<List<PatronMessage>>
    suspend fun markMessageRead(account: Account, id: Int): Result<Unit>
    suspend fun markMessageUnread(account: Account, id: Int): Result<Unit>
    suspend fun markMessageDeleted(account: Account, id: Int): Result<Unit>

    suspend fun fetchPatronCharges(account: Account): Result<PatronCharges>
}
