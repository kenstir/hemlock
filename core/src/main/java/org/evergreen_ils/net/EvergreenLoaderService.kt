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
import net.kenstir.hemlock.logging.Log
import org.evergreen_ils.xdata.XGatewayClient
import org.evergreen_ils.xdata.paramListOf
import org.evergreen_ils.xdata.parseOrgStringSetting
import org.evergreen_ils.xdata.payloadFirstAsObject
import org.evergreen_ils.xdata.payloadFirstAsObjectList
import org.evergreen_ils.xdata.payloadFirstAsString
import net.kenstir.hemlock.net.LoaderServiceOptions
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.Api
import org.evergreen_ils.idl.IDLParser

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

    companion object {
        private val TAG = EvergreenLoaderService::class.java.simpleName
    }
}
