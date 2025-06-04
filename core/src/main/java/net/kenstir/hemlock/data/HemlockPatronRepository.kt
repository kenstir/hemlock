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

package net.kenstir.hemlock.data

import net.kenstir.hemlock.data.models.PatronList
import net.kenstir.hemlock.data.models.PatronListItem

class HemlockPatronRepository(val patronService: PatronService): PatronRepository {

    override suspend fun fetchLists(patronId: Int, authToken: String): Result<List<PatronList>> {
        return patronService.fetchLists(patronId, authToken)
    }

    override suspend fun fetchListItems(
        patronId: Int,
        authToken: String,
        listId: Int
    ): Result<List<PatronListItem>> {
        return patronService.fetchListItems(patronId, authToken, listId)
    }
}
