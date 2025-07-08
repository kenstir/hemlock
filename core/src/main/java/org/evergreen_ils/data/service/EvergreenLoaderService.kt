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

import net.kenstir.data.service.LoaderService
import net.kenstir.data.Result
import net.kenstir.logging.Log
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.gateway.paramListOf
import net.kenstir.data.service.LoadStartupOptions
import net.kenstir.data.jsonMapOf
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.Api
import org.evergreen_ils.idl.IDLParser
import org.evergreen_ils.system.EgSms

object EvergreenLoaderService: LoaderService {
    const val TAG = "LoaderService"

    override suspend fun loadStartupPrerequisites(serviceOptions: LoadStartupOptions): Result<Unit> {
        return try {
            return Result.Success(loadStartupDataImpl(serviceOptions))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadPlaceHoldPrerequisites(): Result<Unit> {
        return try {
            return Result.Success(loadPlaceHoldDataImpl())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadStartupDataImpl(serviceOptions: LoadStartupOptions): Unit = coroutineScope {
        // sync: cache keys must be established first, before IDL is loaded
        GatewayClient.clientCacheKey = serviceOptions.clientCacheKey
        GatewayClient.serverCacheKey = fetchServerCacheKey()

        // sync: Load the IDL next, because everything else depends on it
        var now = System.currentTimeMillis()
        val url = GatewayClient.getIDLUrl()
        val xml = GatewayClient.get(url).bodyAsText()
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
        Log.logElapsedTime(TAG, now, "loadServiceData ${jobs.size} deferreds completed")
    }

    private suspend fun loadPlaceHoldDataImpl(): Unit = coroutineScope {
        var now = System.currentTimeMillis()

        // async: Load all org settings and SMS carriers in parallel
        val jobs = mutableListOf<Deferred<Any>>()
        for (org in EgOrg.visibleOrgs) {
            jobs.add(async {
                EvergreenOrgService.loadOrgSettings(org.id)
            })
        }
        jobs.add(async { loadSmsCarriers() })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        Log.logElapsedTime(TAG, now, "loadPlaceHoldData ${jobs.size} deferreds completed")
    }

    private suspend fun fetchServerCacheKey(): String {
        // fetch the server version
        val response = GatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)
        val serverVersion = response.payloadFirstAsString()

        // fetch the cache key org setting
        val settings = listOf(Api.SETTING_HEMLOCK_CACHE_KEY)
        val params = paramListOf(EgOrg.consortiumID, settings, Api.ANONYMOUS)
        val obj = GatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, params, false)
            .payloadFirstAsObject()
        val hemlockCacheKey = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_CACHE_KEY)

        return if (hemlockCacheKey.isNullOrEmpty()) serverVersion else "$serverVersion-$hemlockCacheKey"
    }

    private suspend fun loadOrgTypes() {
        Log.v(TAG, "loadOrgTypes ...")
        val response = GatewayClient.fetch(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, paramListOf(), true)
        EgOrg.loadOrgTypes(response.payloadFirstAsObjectList())
        Log.v(TAG, "loadOrgTypes ... done")
    }

    private suspend fun loadOrgTree(useHierarchicalOrgTree: Boolean) {
        Log.v(TAG, "loadOrgTree ...")
        val response = GatewayClient.fetch(Api.ACTOR, Api.ORG_TREE_RETRIEVE, paramListOf(), true)
        EgOrg.loadOrgs(response.payloadFirstAsObject(), useHierarchicalOrgTree)
        Log.v(TAG, "loadOrgTree ... done")
    }

    private suspend fun loadCopyStatuses() {
        Log.v(TAG, "loadCopyStatuses ...")
        val response = GatewayClient.fetch(Api.SEARCH, Api.COPY_STATUS_ALL, paramListOf(), true)
        val ret = response.payloadFirstAsObjectList()
        EgCopyStatus.loadCopyStatuses(ret)
        Log.v(TAG, "loadCopyStatuses ... done")
    }

    private suspend fun loadCodedValueMaps() {
        Log.v(TAG, "loadCodedValueMaps ...")
        val formats = listOf(EgCodedValueMap.ICON_FORMAT, EgCodedValueMap.SEARCH_FORMAT)
        val searchParams = jsonMapOf("ctype" to formats)
        val response = GatewayClient.fetch(Api.PCRUD, Api.SEARCH_CCVM, paramListOf(Api.ANONYMOUS, searchParams), true)
        EgCodedValueMap.loadCodedValueMaps(response.payloadFirstAsObjectList())
        Log.v(TAG, "loadCodedValueMaps ... done")
    }

    private suspend fun loadSmsCarriers() {
        Log.v(TAG, "loadSmsCarriers ...")
        val searchParams = jsonMapOf("active" to 1)
        val response = GatewayClient.fetch(Api.PCRUD, Api.SEARCH_SMS_CARRIERS, paramListOf(Api.ANONYMOUS, searchParams), true)
        val carriers = response.payloadFirstAsObjectList()
        EgSms.loadCarriers(carriers)
        Log.v(TAG, "loadSmsCarriers ... done")
    }
}
