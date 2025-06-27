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

package net.kenstir.hemlock.mock

import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.model.PatronList
import net.kenstir.hemlock.data.model.ListItem
import net.kenstir.hemlock.net.UserService

class MockUserService: UserService {
    override fun makeAccount(username: String, authToken: String): Account {
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
        patronList: PatronList,
        queryForVisibleItems: Boolean
    ): Result<Unit> {
        TODO("Not yet implemented")
    }
}
