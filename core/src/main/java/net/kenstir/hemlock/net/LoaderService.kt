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
import net.kenstir.hemlock.data.model.Account

data class LoaderServiceOptions(
    val clientCacheKey: String,
    val useHierarchicalOrgTree: Boolean,
)

/**
 * Service for loading global data
 */
interface LoaderService {
    /**
     * Load any and all prerequisite data required for the client to function.
     * With the exception of AuthService methods, this must be called before
     * any other Service methods.
     *
     * Requires the clientCacheKey, and as a side effect, fetches the server cache key.
     *
     * See <a href="https://kenstir.github.io/hemlock-docs/docs/admin-guide/notes-on-caching">Notes on Caching</a>
     */
    suspend fun loadServiceData(serviceOptions: LoaderServiceOptions): Result<Unit>
}
