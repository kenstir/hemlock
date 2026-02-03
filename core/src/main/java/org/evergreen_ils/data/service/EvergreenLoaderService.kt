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

import android.content.res.Resources
import io.ktor.client.HttpClient
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
import okhttp3.OkHttpClient
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgCopyStatus
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.idl.IDLParser
import org.evergreen_ils.system.EgMessageMap
import org.evergreen_ils.system.EgSms
import java.io.File

object EvergreenLoaderService: LoaderService {
    const val TAG = "LoaderService"

    override var serviceUrl: String
        get() = GatewayClient.baseUrl
        set(value) {
            GatewayClient.baseUrl = value
        }

    override val httpClient: HttpClient
        get() = GatewayClient.client

    override val okHttpClient: OkHttpClient
        get() = GatewayClient.okHttpClient

    override fun initHttpClient(cacheDir: File): OkHttpClient {
        GatewayClient.cacheDirectory = cacheDir
        GatewayClient.initHttpClient()
        return GatewayClient.okHttpClient
    }

    override suspend fun loadStartupPrerequisites(serviceOptions: LoadStartupOptions, resources: Resources): Result<Unit> {
        return try {
            return Result.Success(loadStartupDataImpl(serviceOptions, resources))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadStartupDataImpl(serviceOptions: LoadStartupOptions, resources: Resources): Unit = coroutineScope {
        // sync: cache keys must be established first, before IDL is loaded
        GatewayClient.clientCacheKey = serviceOptions.clientCacheKey
        val serverCacheKey = loadGlobalOrgSettings()
        GatewayClient.serverCacheKey = serverCacheKey

        // sync: Load the IDL next, because everything else depends on it
        var now = System.currentTimeMillis()
        val url = GatewayClient.getIDLUrl()
        val xml = GatewayClient.get(url).bodyAsText()
        val parser = IDLParser(xml.byteInputStream())
        now = Log.logElapsedTime(TAG, now, "[init] loadStartupData IDL fetched")
        parser.parse()
        now = Log.logElapsedTime(TAG, now, "[init] loadStartupData IDL parsed")

        // async: Load the rest of the data in parallel
        val jobs = mutableListOf<Deferred<Any>>()
        jobs.add(async { loadOrgTypes() })
        jobs.add(async { loadOrgTree(serviceOptions.useHierarchicalOrgTree) })
        jobs.add(async { loadCopyStatuses() })
        jobs.add(async { loadCodedValueMaps() })
        jobs.add(async { EgMessageMap.init(resources) })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        Log.logElapsedTime(TAG, now, "[init] loadStartupData ${jobs.size} deferreds completed")
    }

    override suspend fun loadPlaceHoldPrerequisites(): Result<Unit> {
        return try {
            return Result.Success(loadPlaceHoldDataImpl())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadPlaceHoldDataImpl(): Unit = coroutineScope {
        val now = System.currentTimeMillis()

        // async: Load all org settings and SMS carriers in parallel
        val jobs = mutableListOf<Deferred<Any>>()
        for (org in EgOrg.visibleOrgs) {
            jobs.add(async {
                EvergreenConsortiumService.loadOrgSettings(org.id)
            })
        }
        jobs.add(async { loadSmsCarriers() })

        // await all deferred (see awaitAll doc for differences)
        jobs.map { it.await() }
        Log.logElapsedTime(TAG, now, "loadPlaceHoldData ${jobs.size} deferreds completed")
    }

    // Load global org settings from consortium org and return serverCacheKey
    private suspend fun loadGlobalOrgSettings(): String {
        // fetch the server version
        val response = GatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)
        val serverVersion = response.payloadFirstAsString()

        // fetch the global org settings
        val settings = listOf(
            Api.SETTING_HEMLOCK_CACHE_KEY,
            Api.SETTING_OPAC_ALERT_BANNER_SHOW,
            Api.SETTING_OPAC_ALERT_BANNER_TEXT,
            Api.SETTING_SMS_ENABLE,
        )
        val params = paramListOf(EgOrg.CONSORTIUM_ID, settings, Api.ANONYMOUS)
        val obj = GatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, params, false)
            .payloadFirstAsObject()

        // load global settings
        EgOrg.alertBannerEnabled = obj.getBooleanValueFromOrgSetting(Api.SETTING_OPAC_ALERT_BANNER_SHOW) ?: false
        EgOrg.alertBannerText = obj.getStringValueFromOrgSetting(Api.SETTING_OPAC_ALERT_BANNER_TEXT)
        EgOrg.smsEnabled = obj.getBooleanValueFromOrgSetting(Api.SETTING_SMS_ENABLE) ?: false

        // derive and return serverCacheKey
        val hemlockCacheKey = obj.getStringValueFromOrgSetting(Api.SETTING_HEMLOCK_CACHE_KEY)
        val serverCacheKey = if (hemlockCacheKey.isNullOrEmpty()) serverVersion else "$serverVersion-$hemlockCacheKey"
        return serverCacheKey
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

    override suspend fun fetchPublicIpAddress(): Result<String> {
        return try {
            val url = "https://api.ipify.org"
            val ip = GatewayClient.get(url).bodyAsText()
            Result.Success(ip)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
