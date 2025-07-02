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

package org.evergreen_ils.net

import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import net.kenstir.hemlock.data.model.BibRecord
import net.kenstir.hemlock.data.model.CopyLocationCounts
import net.kenstir.hemlock.net.SearchResults
import net.kenstir.hemlock.net.SearchService
import org.evergreen_ils.Api
import org.evergreen_ils.data.EvergreenCopyLocationCounts
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.system.EgSearch.selectedOrganization
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.XOSRFObject
import org.evergreen_ils.xdata.paramListOf

class EvergreenSearchService: SearchService {

    // Build query string, taken with a grain of salt from
    // https://wiki.evergreen-ils.org/doku.php?id=documentation:technical:search_grammar
    // e.g. "title:Harry Potter chamber of secrets search_format(book) site(MARLBORO)"
    override fun makeQueryString(searchText: String, searchClass: String?, searchFormat: String?, sort: String?): String {
        val sb = StringBuilder()
        sb.append(searchClass).append(":").append(searchText)
        if (!searchFormat.isNullOrEmpty()) sb.append(" search_format(").append(searchFormat).append(")")
        if (selectedOrganization != null) sb.append(" site(").append(selectedOrganization!!.shortname).append(")")
        if (!sort.isNullOrEmpty()) sb.append(" sort(").append(sort).append(")")
        return sb.toString()
    }

    override suspend fun searchCatalog(queryString: String, limit: Int): Result<SearchResults> {
        return try {
            EgSearch.searchLimit = limit
            val result = fetchMulticlassQuery(queryString, limit, true)
            when (result) {
                is Result.Error -> return Result.Error(result.exception)
                is Result.Success -> {}
            }
            val ret = EgSearch.loadResults(result.get())
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getLastSearchResults(): SearchResults {
        return EgSearch.lastResults
    }

    override suspend fun fetchCopyLocationCounts(recordId: Int, orgId: Int, orgLevel: Int): Result<List<CopyLocationCounts>> {
        return try {
            val response = XGatewayClient.fetch(Api.SEARCH, Api.COPY_LOCATION_COUNTS, paramListOf(recordId, orgId, orgLevel), false)
            val clcList = response.payloadFirstAsList()
            val ret = EvergreenCopyLocationCounts.makeArray(clcList)
            Result.Success(ret)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchMulticlassQuery(queryString: String, limit: Int, shouldCache: Boolean): Result<XOSRFObject> {
        return try {
            val options = jsonMapOf("limit" to limit, "offset" to 0)
            val requestServerSideCaching = 0
            val params = paramListOf(options, queryString, requestServerSideCaching)
            val response = XGatewayClient.fetch(Api.SEARCH, Api.MULTICLASS_QUERY, params, shouldCache)
            Result.Success(response.payloadFirstAsObject())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
