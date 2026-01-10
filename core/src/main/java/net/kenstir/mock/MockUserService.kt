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

package net.kenstir.mock

import net.kenstir.data.model.PatronMessage
import net.kenstir.data.Result
import net.kenstir.data.model.Account
import net.kenstir.data.model.PatronList
import net.kenstir.data.model.PatronCharges
import net.kenstir.data.service.UserService

object MockUserService: UserService {
    override fun makeAccount(username: String, authToken: String): Account {
        TODO("Not yet implemented")
    }

    override fun payFinesUrl(account: Account): String {
        TODO("Not yet implemented")
    }

    override fun isPayFinesEnabled(account: Account): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun loadUserSession(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteSession(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun loadPatronLists(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun loadPatronListItems(
        account: Account,
        patronList: PatronList
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun createPatronList(account: Account, name: String, description: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deletePatronList(account: Account, listId: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun addItemToPatronList(account: Account, listId: Int, recordId: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun removeItemFromPatronList(account: Account, listId: Int, itemId: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun enableCheckoutHistory(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun disableCheckoutHistory(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun clearCheckoutHistory(account: Account): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun changePickupOrg(account: Account, orgId: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updatePushNotificationToken(account: Account, token: String?): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPatronMessages(account: Account): Result<List<PatronMessage>> {
        TODO("Not yet implemented")
    }

    override suspend fun markMessageRead(account: Account, id: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun markMessageUnread(account: Account, id: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun markMessageDeleted(account: Account, id: Int): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPatronCharges(account: Account): Result<PatronCharges> {
        TODO("Not yet implemented")
    }
}
