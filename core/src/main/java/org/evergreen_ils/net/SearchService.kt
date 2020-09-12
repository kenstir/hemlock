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

import org.evergreen_ils.data.Result
import org.opensrf.util.OSRFObject

interface SearchService {
    suspend fun fetchAssetCopy(copyId: Int): Result<OSRFObject>
    suspend fun fetchAssetCallNumber(callNumber: Int): Result<OSRFObject>
    suspend fun fetchCopyLocationCounts(id: Int, orgId: Int, level: Int): Result<List<Any>>
    suspend fun fetchCopyStatuses(): Result<List<OSRFObject>>
    suspend fun fetchCopySummary(id: Int, orgId: Int): Result<List<OSRFObject>>
    suspend fun fetchCopyMODS(copyId: Int): Result<OSRFObject>
    suspend fun fetchRecordMODS(id: Int): Result<OSRFObject>
    suspend fun fetchMetarecordMODS(id: Int): Result<OSRFObject>
    suspend fun fetchHoldParts(id: Int): Result<List<OSRFObject>>
    suspend fun fetchMulticlassQuery(queryString: String, limit: Int): Result<OSRFObject>
}
