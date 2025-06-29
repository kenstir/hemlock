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

import android.text.TextUtils
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.jsonMapOf
import net.kenstir.hemlock.data.model.BibRecord
import net.kenstir.hemlock.net.SearchResults
import net.kenstir.hemlock.net.SearchService
import org.evergreen_ils.Api
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.system.EgSearch.selectedOrganization
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.XOSRFObject
import org.evergreen_ils.xdata.paramListOf

class EvergreenSearchService: SearchService {
    suspend fun fetchAssetCopy(copyId: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    suspend fun fetchAssetCallNumber(callNumber: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    suspend fun fetchCopyLocationCounts(id: Int, orgId: Int, orgLevel: Int): Result<List<Any>> {
        TODO("Not yet implemented")
    }

    suspend fun fetchCopyCount(id: Int, orgId: Int): Result<List<XOSRFObject>> {
        TODO("Not yet implemented")
    }

    suspend fun fetchCopyMODS(copyId: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    suspend fun fetchRecordMODS(id: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    suspend fun fetchMetarecordMODS(id: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    suspend fun fetchHoldParts(id: Int): Result<List<XOSRFObject>> {
        TODO("Not yet implemented")
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
}

class EvergreenSearchResults: SearchResults {
    override val numResults: Int
        get() = EgSearch.results.size

    override val totalMatches: Int
        get() = EgSearch.visible
}
