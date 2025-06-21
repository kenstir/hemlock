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

package net.kenstir.hemlock.data.evergreen

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

import net.kenstir.hemlock.data.InitService
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.android.Log
import org.evergreen_ils.data.parseOrgStringSetting
import org.evergreen_ils.system.EgOrg
import org.open_ils.idl.IDLParser

private val TAG = EvergreenInitService::class.java.simpleName

class EvergreenInitService : InitService {

    override suspend fun fetchServerCacheKey(): Result<String> {
        return try {
            // fetch the server version
            val response = XGatewayClient.fetch(Api.ACTOR, Api.ILS_VERSION, paramListOf(), false)
            val serverVersion = response.payloadFirstAsString()

            // fetch the cache key org setting
            val settings = listOf(Api.SETTING_HEMLOCK_CACHE_KEY)
            val params = paramListOf(EgOrg.consortiumID, settings, Api.ANONYMOUS)
            val obj = XGatewayClient.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, params, false)
                .payloadFirstAsObject()
            val hemlockCacheKey = parseOrgStringSetting(obj, Api.SETTING_HEMLOCK_CACHE_KEY)

            val serverCacheKey = if (hemlockCacheKey.isNullOrEmpty()) serverVersion else "$serverVersion-$hemlockCacheKey"
            Result.Success(serverCacheKey)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun loadServiceData(): Result<Unit> {
        return try {
            return Result.Success(loadServiceDataImpl())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun loadServiceDataImpl(): Unit = coroutineScope {
        // sync: Load the IDL first, because everything else depends on it
        var now = System.currentTimeMillis()
        val url = XGatewayClient.getIDLUrl()
        val xml = XGatewayClient.get(url).bodyAsText()
        val parser = IDLParser(xml.byteInputStream())
        now = Log.logElapsedTime(TAG, now, "loadIDL.get")
        parser.parse()
        Log.logElapsedTime(TAG, now, "loadIDL.parse")

        // async: Load the rest of the data in parallel
        val job1 = async {
            loadOrgTypes()
        }
        val job2 = async {
            Log.d(TAG, "job2: start")
            delay(1000);
            Log.d(TAG, "job2: end")
        }
        val job3 = async {
            Log.d(TAG, "job3: start")
            delay(200);
            Log.d(TAG, "job3: end")
        }

        val results = listOf<Unit>(job1.await(), job2.await(), job3.await())
//        val result = results.firstOrNull { it is Result.Error }
//            ?: Result.Success(Unit)
//        result
    }

    private suspend fun loadOrgTypes() {
        Log.d(TAG, "job1: loading org types")
        val response = XGatewayClient.fetch(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, paramListOf(), true)
        EgOrg.loadOrgTypes(response.payloadFirstAsObjectList())
        Log.d(TAG, "job1: loading org types done")
    }

    private suspend fun loadOrgTree(): Result<Unit> {
        TODO()
    }

    private suspend fun loadCopyStatuses(): Result<Unit> {
        TODO()
    }

    private suspend fun loadCodedValueMaps(): Result<Unit> {
        TODO()
    }
}
