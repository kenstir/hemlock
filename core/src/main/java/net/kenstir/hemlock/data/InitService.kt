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

/**
 * Service methods required to initialize the client.  Except for AuthService,
 * these methods must be called before most other Service methods.
 */
interface InitService {
    /**
     * Returns a string to be used in the a cache-busting param for GET requests.
     * For Evergreen, this is composed of the server version and optional cache key.
     *
     * See <a href="https://kenstir.github.io/hemlock-docs/docs/admin-guide/notes-on-caching">Notes on Caching</a>
     */
    suspend fun fetchServerCacheKey(): Result<String>

    /**
     * Initializes the service by loading any required data.
     * fetchServerCacheKey() must be called before this method.
     */
    suspend fun loadServiceData(): Result<Unit>
}
