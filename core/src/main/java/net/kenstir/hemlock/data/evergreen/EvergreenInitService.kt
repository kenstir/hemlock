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

import net.kenstir.hemlock.data.InitService
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.android.Log
import org.open_ils.idl.IDLParser

private val TAG = EvergreenInitService::class.java.simpleName

class EvergreenInitService : InitService {

    override suspend fun initializeServiceData(): Result<Unit> {
        return try {
            // Load the IDL
            var now = System.currentTimeMillis()
            val url = XGatewayClient.getIDLUrl()
            val xml = XGatewayClient.get(url).bodyAsText()
            val parser = IDLParser(xml.byteInputStream())
            now = Log.logElapsedTime(TAG, now, "loadIDL.get")
            parser.parse()
            Log.logElapsedTime(TAG, now, "loadIDL.parse")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
