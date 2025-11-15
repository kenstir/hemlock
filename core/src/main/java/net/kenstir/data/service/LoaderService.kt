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

data class LoadStartupOptions(
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
     * In Evergreen, this includes the serverCacheKey and the IDL.
     *
     * Requires the clientCacheKey, and as a side effect, fetches the server cache key.
     *
     * See <a href="https://kenstir.github.io/hemlock-docs/docs/admin-guide/notes-on-caching">Notes on Caching</a>
     */
    suspend fun loadStartupPrerequisites(serviceOptions: LoadStartupOptions): Result<Unit>

    /**
     * Load any additional data that is required for the Place Hold activity.
     *
     * In Evergreen, this includes the settings of every org and the list of SMS Carriers.
     */
    suspend fun loadPlaceHoldPrerequisites(): Result<Unit>

    /**
     * Get the client's public IP address.
     *
     * Many consortia are now using IP-based access control, so include this in error reports
     * to help identify issues.
     */
    suspend fun fetchPublicIpAddress(): Result<String>
}
