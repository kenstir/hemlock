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
import net.kenstir.hemlock.data.SearchService
import net.kenstir.hemlock.data.evergreen.XOSRFObject

class EvergreenSearchService: SearchService {
    override suspend fun fetchAssetCopy(copyId: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAssetCallNumber(callNumber: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCopyLocationCounts(id: Int, orgId: Int, orgLevel: Int): Result<List<Any>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCopyCount(id: Int, orgId: Int): Result<List<XOSRFObject>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCopyMODS(copyId: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchRecordMODS(id: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMetarecordMODS(id: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchHoldParts(id: Int): Result<List<XOSRFObject>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMulticlassQuery(queryString: String, limit: Int): Result<XOSRFObject> {
        TODO("Not yet implemented")
    }
}
