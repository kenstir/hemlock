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
import net.kenstir.data.model.Account
import net.kenstir.data.model.Organization

/**
 * Service for loading organization (library) settings and details.
 */
interface OrgService {

    /**
     * An ID to use for the consortium as a whole, when needed.
     */
    val consortiumID: Int

    /** Is SMS notifications enabled for all orgs? */
    val isSmsEnabled: Boolean

    /**
     * Find an org by its orgId.
     */
    fun findOrg(orgID: Int?): Organization?

    /**
     * Find an org and return its shortName, returning a safe default if not found.
     */
    fun getOrgShortNameSafe(orgID: Int?): String

    /**
     * Find an org and return its name, returning a safe default if not found.
     */
    fun getOrgNameSafe(orgID: Int?): String

    /** Returns a list of all visible orgs */
    fun getVisibleOrgs(): List<Organization>

    /** Returns a list of the labels of all visible orgs for use in a Spinner */
    fun getOrgSpinnerLabels(): List<String>

    /** Returns a list of the shortnames of all visible orgs */
    fun getOrgSpinnerShortNames(): List<String>

    /** Logs details about all loaded orgs for debugging */
    fun dumpOrgStats()

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
