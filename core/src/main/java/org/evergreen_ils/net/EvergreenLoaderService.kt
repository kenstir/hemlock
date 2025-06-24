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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

import net.kenstir.hemlock.net.LoaderService
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.android.Log
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.paramListOf
import org.evergreen_ils.xdata.parseOrgStringSetting
import org.evergreen_ils.xdata.payloadFirstAsObject
import org.evergreen_ils.xdata.payloadFirstAsObjectList
import org.evergreen_ils.xdata.payloadFirstAsString
import net.kenstir.hemlock.net.LoaderServiceOptions
import net.kenstir.hemlock.data.jsonMapOf
import net.kenstir.hemlock.data.model.Account
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.Api
import org.evergreen_ils.idl.IDLParser
import org.evergreen_ils.model.EvergreenOrganization

class EvergreenLoaderService: LoaderService {

    override suspend fun loadServiceData(serviceOptions: LoaderServiceOptions): Result<Unit> {
        return try {
            return Result.Success(loadServiceDataImpl(serviceOptions))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadServiceDataImpl(serviceOptions: LoaderServiceOptions): Unit = coroutineScope {
        // sync: cache keys must be established first, before IDL is loaded
        XGatewayClient.clientCacheKey = serviceOptions.clientCacheKey
        XGatewayClient.serverCacheKey = fetchServerCacheKey()

        // sync: Load the IDL next, because everything else depends on it
        var now = System.currentTimeMillis()
        val url = XGatewayClient.getIDLUrl()
        val xml = XGatewayClient.get(url).bodyAsText()
        val parser = IDLParser(xml.byteInputStream())
        now = Log.logElapsedTime(TAG, now, "loadServiceData IDL fetched")
        parser.parse()
        now = Log.logElapsedTime(TAG, now, "loadServiceData IDL parsed")

        // async: Load the rest of the data in parallel
        val jobs = mutableListOf<Deferred<Any>>()
        jobs.add(async { loadOrgTypes() })
        jobs.add(async { loadOrgTree(serviceOptions.useHierarchicalOrgTree) })
        jobs.add(async { loadCopyStatuses() })
        jobs.add(async { loadCodedValueMaps() })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        now = Log.logElapsedTime(TAG, now, "loadServiceData ${jobs.size} deferreds completed")
    }

    private suspend fun fetchServerCacheKey(): String {
        // fetch the server version
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)
        val serverVersion = response.payloadFirstAsString()

        // fetch the cache key org setting
        val settings = listOf(Api.SETTING_HEMLOCK_CACHE_KEY)
        val params = paramListOf(EgOrg.consortiumID, settings, Api.ANONYMOUS)
        val obj = XGatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, params, false)
            .payloadFirstAsObject()
        val hemlockCacheKey = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_CACHE_KEY)

        return if (hemlockCacheKey.isNullOrEmpty()) serverVersion else "$serverVersion-$hemlockCacheKey"
    }

    private suspend fun loadOrgTypes() {
        Log.d(TAG, "loading org types ...")
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, paramListOf(), true)
        EgOrg.loadOrgTypes(response.payloadFirstAsObjectList())
        Log.d(TAG, "loading org types ... done")
    }

    private suspend fun loadOrgTree(useHierarchicalOrgTree: Boolean) {
        Log.d(TAG, "loading org tree ...")
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ORG_TREE_RETRIEVE, paramListOf(), true)
        EgOrg.loadOrgs(response.payloadFirstAsObject(), useHierarchicalOrgTree)
        Log.d(TAG, "loading org tree ... done")
    }

    private suspend fun loadCopyStatuses() {
        Log.d(TAG, "loading copy statuses ...")
        val response = XGatewayClient.fetch(Api.SEARCH, Api.COPY_STATUS_ALL, paramListOf(), true)
        val ret = response.payloadFirstAsObjectList()
        EgCopyStatus.loadCopyStatuses(ret)
        Log.d(TAG, "loading copy statuses ... done")
    }

    private suspend fun loadCodedValueMaps() {
        Log.d(TAG, "loading coded value maps ...")
        val formats = listOf(EgCodedValueMap.ICON_FORMAT, EgCodedValueMap.SEARCH_FORMAT)
        val searchParams = jsonMapOf("ctype" to formats)
        val response = XGatewayClient.fetch(Api.PCRUD, Api.SEARCH_CCVM, paramListOf(Api.ANONYMOUS, searchParams), true)
        EgCodedValueMap.loadCodedValueMaps(response.payloadFirstAsObjectList())
        Log.d(TAG, "loading coded value maps ... done")
    }

    override suspend fun loadOrgSettings(orgID: Int): Result<Unit> {
        return try {
            return Result.Success(loadOrgSettingsImpl(orgID))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadOrgSettingsImpl(orgID: Int) {
        Log.d(TAG, "loading org settings for org $orgID ...")
        val org = EgOrg.findOrg(orgID) as? EvergreenOrganization
            ?: throw IllegalArgumentException("Org $orgID not found")
        val settings = mutableListOf(
            Api.SETTING_CREDIT_PAYMENTS_ALLOW,
            Api.SETTING_INFO_URL,
            Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
            Api.SETTING_HEMLOCK_ERESOURCES_URL,
            Api.SETTING_HEMLOCK_EVENTS_URL,
            Api.SETTING_HEMLOCK_MEETING_ROOMS_URL,
            Api.SETTING_HEMLOCK_MUSEUM_PASSES_URL,
        )
        if (orgID == EgOrg.consortiumID)
            settings.add(Api.SETTING_SMS_ENABLE)
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, paramListOf(orgID, settings, Api.ANONYMOUS), true)
        val obj = response.payloadFirstAsObject()
        org.loadSettings(obj)
        Log.d(TAG, "loading org settings for org $orgID ... done")
    }

    override suspend fun loadOrgDetails(orgID: Int): Result<Unit> {
        return try {
            return Result.Success(loadOrgDetailsImpl(orgID))
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
        jobs.add(async { loadOrgAddress(account, org) })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        Log.d(TAG, "loading org details for org $orgID ... done")
    }

    private suspend fun loadOrgHours(account: Account, org: EvergreenOrganization) {
        Log.d(TAG, "loading org hours for org ${org.id} ...")
        val (authToken, _) = account.getCredentialsOrThrow()
        val response = XGatewayClient.fetch(Api.ACTOR, Api.HOURS_OF_OPERATION_RETRIEVE, paramListOf(authToken, org.id), false)
        org.loadHours(response.payloadFirstAsOptionalObject())
        Log.d(TAG, "loading org hours for org ${org.id} ... done")
    }

    private suspend fun loadOrgClosures(account: Account, org: EvergreenOrganization) {
        Log.d(TAG, "loading org closures for org ${org.id} ...")
        val (authToken, _) = account.getCredentialsOrThrow()
        // Neither the default start_date in ClosedDates::fetch_dates nor the start_date
        // in [param] is working to limit the results; we get all closures since day 1.
        val options = jsonMapOf("orgid" to org.id)
        val response = XGatewayClient.fetch(Api.ACTOR, Api.HOURS_CLOSED_RETRIEVE, paramListOf(authToken, options), false)
        org.loadClosures(response.payloadFirstAsObjectList())
        Log.d(TAG, "loading org closures for org ${org.id} ... done")
    }

    private suspend fun loadOrgAddress(org: EvergreenOrganization) {
        Log.d(TAG, "loading org address for org ${org.id} ...")
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ADDRESS_RETRIEVE, paramListOf(org.addressID), false)
        org.loadAddress(response.payloadFirstAsObject()))
        Log.d(TAG, "loading org address for org ${org.id} ... done")
    }

    companion object {
        private val TAG = EvergreenLoaderService::class.java.simpleName
    }
}
