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

import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.CopyLocationCounts

interface SearchService {
    /**
     * Construct a query string for [searchCatalog]
     */
    fun makeQueryString(searchText: String, searchClass: String?, searchFormat: String?, sort: String?): String

    /**
     * Search the catalog for records matching [queryString]
     */
    suspend fun searchCatalog(queryString: String, limit: Int): Result<SearchResults>

    /**
     * Return the results from the last search
     */
    fun getLastSearchResults(): SearchResults

    /**
     * find copy location counts for a given [recordId], at [orgId] level and below
     */
    suspend fun fetchCopyLocationCounts(recordId: Int, orgId: Int, orgLevel: Int): Result<List<CopyLocationCounts>>
}

interface SearchResults {
    /** number of results returned */
    val numResults: Int

    /** total number of matches in the catalog, may be higher than [numResults] if search was limited */
    val totalMatches: Int

    /** matching records
     *
     * These records are skeletons, and do not have Details, Attributes, or CopyCounts loaded
     */
    val records: List<BibRecord>
}
