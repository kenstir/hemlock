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

package org.evergreen_ils.data.service

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.kenstir.data.Result
import net.kenstir.data.jsonMapOf
import net.kenstir.data.model.Account
import net.kenstir.data.model.Organization
import net.kenstir.data.model.SMSCarrier
import net.kenstir.data.service.ConsortiumService
import net.kenstir.logging.Log
import org.evergreen_ils.Api
import org.evergreen_ils.data.model.EvergreenOrganization
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.paramListOf
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.system.EgSms
import org.evergreen_ils.util.getCredentialsOrThrow

object EvergreenConsortiumService: ConsortiumService {
    const val TAG = "ConsortService"

    override val consortiumID = EgOrg.CONSORTIUM_ID

    override val isSmsEnabled: Boolean
        get() = EgOrg.smsEnabled

    override val alertBanner: String?
        get() = if (EgOrg.alertBannerEnabled) EgOrg.alertBannerText else null

    override var selectedOrganization: Organization?
        get() = EgSearch.selectedOrganization
        set(value) {
            EgSearch.selectedOrganization = value
        }

    override val searchFormatSpinnerLabels: List<String>
        get() = EgCodedValueMap.searchFormatSpinnerLabels

    override val searchFormatSpinnerValues: List<String>
        get() = EgCodedValueMap.searchFormatSpinnerValues

    override val smsCarriers: List<SMSCarrier>
        get() = EgSms.carriers

    override val smsCarrierSpinnerLabels: List<String>
        get() = EgSms.spinnerLabels

    override val smsCarrierSpinnerValues: List<String>
        get() = EgSms.spinnerValues

    override fun findOrg(orgID: Int?) = EgOrg.findOrg(orgID)

    override fun findOrgShortNameSafe(orgID: Int?) = EgOrg.getOrgShortNameSafe(orgID)

    override fun findOrgNameSafe(orgID: Int?) = EgOrg.getOrgNameSafe(orgID)

    override val visibleOrgs: List<Organization>
        get() = EgOrg.visibleOrgs

    override val orgSpinnerLabels: List<String>
        get() = EgOrg.orgSpinnerLabels()

    override val orgSpinnerShortNames: List<String>
        get() = EgOrg.spinnerShortNames()

    override fun dumpOrgStats() = EgOrg.dumpOrgStats()

    override suspend fun loadOrgSettings(orgID: Int): Result<Unit> {
        return try {
            return Result.Success(loadOrgSettingsImpl(orgID))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun loadOrgSettingsImpl(orgID: Int) {
        Log.v(TAG, "[orgs] id:$orgID load settings ...")
        val org = EgOrg.findOrg(orgID) as? EvergreenOrganization
            ?: throw IllegalArgumentException("Org $orgID not found")
        if (org.settingsLoaded)
            return
        val settings = mutableListOf(
            Api.SETTING_CREDIT_PAYMENTS_ALLOW,
            Api.SETTING_INFO_URL,
            Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
            Api.SETTING_HEMLOCK_ERESOURCES_URL,
            Api.SETTING_HEMLOCK_EVENTS_URL,
            Api.SETTING_HEMLOCK_MEETING_ROOMS_URL,
            Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL,
        )
        val response = GatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, paramListOf(orgID, settings, Api.ANONYMOUS), true)
        val obj = response.payloadFirstAsObject()
        org.loadSettings(obj)
        Log.v(TAG, "[orgs] id:$orgID load settings ... done")
    }

    override suspend fun loadOrgDetails(account: Account, orgID: Int): Result<Unit> {
        return try {
            return Result.Success(loadOrgDetailsImpl(account, orgID))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadOrgDetailsImpl(account: Account, orgID: Int) = coroutineScope {
        Log.d(TAG, "loading org details for org $orgID ...")
        val org = EgOrg.findOrg(orgID) as? EvergreenOrganization
            ?: throw IllegalArgumentException("Org $orgID not found")

        val jobs = mutableListOf<Deferred<Any>>()
        jobs.add(async { loadOrgHours(account, org) })
        jobs.add(async { loadOrgClosures(account, org) })
        jobs.add(async { loadOrgAddress(org) })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        Log.d(TAG, "loading org details for org $orgID ... done")
    }

    private suspend fun loadOrgHours(account: Account, org: EvergreenOrganization) {
        Log.d(TAG, "loading org hours for org ${org.id} ...")
        val (authToken, _) = account.getCredentialsOrThrow()
        val response = GatewayClient.fetch(Api.ACTOR, Api.HOURS_OF_OPERATION_RETRIEVE, paramListOf(authToken, org.id), false)
        org.loadHours(response.payloadFirstAsObjectOrNull())
        Log.d(TAG, "loading org hours for org ${org.id} ... done")
    }

    private suspend fun loadOrgClosures(account: Account, org: EvergreenOrganization) {
        Log.d(TAG, "loading org closures for org ${org.id} ...")
        val (authToken, _) = account.getCredentialsOrThrow()
        // Neither the default start_date in ClosedDates::fetch_dates nor the start_date
        // in [param] is working to limit the results; we get all closures since day 1.
        val options = jsonMapOf("orgid" to org.id)
        val response = GatewayClient.fetch(Api.ACTOR, Api.HOURS_CLOSED_RETRIEVE, paramListOf(authToken, options), false)
        org.loadClosures(response.payloadFirstAsObjectList())
        Log.d(TAG, "loading org closures for org ${org.id} ... done")
    }

    private suspend fun loadOrgAddress(org: EvergreenOrganization) {
        if (org.addressID == null)
            return
        Log.d(TAG, "loading org address for org ${org.id} ...")
        val response = GatewayClient.fetch(Api.ACTOR, Api.ADDRESS_RETRIEVE, paramListOf(org.addressID), false)
        org.loadAddress(response.payloadFirstAsObjectOrNull())
        Log.d(TAG, "loading org address for org ${org.id} ... done")
    }
}
