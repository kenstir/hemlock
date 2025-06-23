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

package org.evergreen_ils.xdata

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import net.kenstir.hemlock.net.elapsedTime
import net.kenstir.hemlock.net.isCached

class XGatewayResponse(val response: HttpResponse) {
    val isCached: Boolean
        get() = isCached(response)

    val elapsed: Long
        get() = elapsedTime(response)

    suspend fun bodyAsText(): String {
        return response.bodyAsText()
    }
}

suspend fun XGatewayResponse.payloadFirstAsObject(): XOSRFObject {
    val json = bodyAsText()
    return XGatewayResult.create(json).payloadFirstAsObject()
}

suspend fun XGatewayResponse.payloadFirstAsObjectList(): List<XOSRFObject> {
    val json = bodyAsText()
    return XGatewayResult.create(json).payloadFirstAsObjectList()
}

suspend fun XGatewayResponse.payloadFirstAsString(): String {
    val json = bodyAsText()
    return XGatewayResult.create(json).payloadFirstAsString()
}
