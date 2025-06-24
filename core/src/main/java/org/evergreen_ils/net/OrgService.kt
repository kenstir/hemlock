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
import net.kenstir.hemlock.data.model.Account

/**
 * Service for loading organization (library) settings and details.
 */
interface OrgService {

    /**
     * Load org settings, e.g. eventsUrl and isPickupLocation.
     * In Evergreen, this requires a specific round-trip for each org.
     */
    suspend fun loadOrgSettings(orgID: Int): Result<Unit>

    /**
     * Load the details required for the OrgDetails screen, e.g. address, hours, and closures.
     * In Evergreen, this requires multiple round-trips per org.
     */
    suspend fun loadOrgDetails(account: Account, orgID: Int): Result<Unit>
}
