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
import net.kenstir.data.model.SMSCarrier

/**
 * Service for loading and finding information about the consortium or library system:
 * organizations (libraries) and their settings and details.
 */
interface ConsortiumService {

    /** An ID to use for the consortium as a whole, when needed. */
    val consortiumID: Int

    /** Is SMS notifications enabled for all orgs? */
    val isSmsEnabled: Boolean

    /** system alert banner */
    val alertBanner: String?

    /** last searched organization */
    var selectedOrganization: Organization?

    /** search format labels for use in a Spinner */
    val searchFormatSpinnerLabels: List<String>

    /** search format codes for use as Spinner values */
    val searchFormatSpinnerValues: List<String>

    /** SMS carriers */
    val smsCarriers: List<SMSCarrier>

    /** SMS carrier spinner labels */
    val smsCarrierSpinnerLabels: List<String>

    /** SMS carrier spinner values */
    val smsCarrierSpinnerValues: List<String>

    /** all visible orgs */
    val visibleOrgs: List<Organization>

    /** all visible org labels for use in a Spinner */
    val orgSpinnerLabels: List<String>

    /** all visible org shortnames for use in Spinner values */
    val orgSpinnerShortNames: List<String>

    /** Finds an org by its orgId. */
    fun findOrg(orgID: Int?): Organization?

    /** Finds an org and return its shortname, returning a safe default if not found. */
    fun findOrgShortNameSafe(orgID: Int?): String

    /** Finds an org and return its name, returning a safe default if not found. */
    fun findOrgNameSafe(orgID: Int?): String

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
