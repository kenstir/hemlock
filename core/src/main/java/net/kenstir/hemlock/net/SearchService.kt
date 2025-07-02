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

package net.kenstir.hemlock.net

import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.model.CopyLocationCounts

interface SearchService {
    suspend fun searchCatalog(queryString: String, limit: Int): Result<SearchResults>
    fun makeQueryString(searchText: String, searchClass: String?, searchFormat: String?, sort: String?): String
    /**
     * find copy location counts for a given [record], at [orgId] level and below
     */
    suspend fun fetchCopyLocationCounts(recordId: Int, orgId: Int, orgLevel: Int): Result<List<CopyLocationCounts>>
}

interface SearchResults {
    val numResults: Int
    val totalMatches: Int
}
