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
import net.kenstir.hemlock.data.ShouldNotHappenException
import org.open_ils.idl.IDLParser

private val TAG = EvergreenInitService::class.java.simpleName

class EvergreenInitService : InitService {

    override suspend fun initializeServiceData(): Result<Unit> {
        return try {
            return initializeServiceDataImpl()
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun initializeServiceDataImpl(): Result<Unit> = coroutineScope {
        // sync: Load the IDL first, because everything else depends on it
        var now = System.currentTimeMillis()
        val url = XGatewayClient.getIDLUrl()
        val xml = XGatewayClient.get(url).bodyAsText()
        val parser = IDLParser(xml.byteInputStream())
        now = Log.logElapsedTime(TAG, now, "loadIDL.get")
        parser.parse()
        Log.logElapsedTime(TAG, now, "loadIDL.parse")

        // Example suspend functions
        val job1 = async {
            Log.d(TAG, "job1: start")
            delay(100);
            Log.d(TAG, "job1: end")
            return@async Result.Error(ShouldNotHappenException("Example error from job1"))
        }
        val job2 = async {
            Log.d(TAG, "job2: start")
            delay(1000);
            Log.d(TAG, "job2: end")
            return@async Result.Success(Unit)
        }
        val job3 = async {
            Log.d(TAG, "job3: start")
            delay(200);
            Log.d(TAG, "job3: end")
            return@async Result.Success(Unit)
        }


        val results = listOf<Result<Unit>>(job1.await(), job2.await(), job3.await())
        val result = results.firstOrNull { it is Result.Error }
            ?: Result.Success(Unit)
        result
    }
}
