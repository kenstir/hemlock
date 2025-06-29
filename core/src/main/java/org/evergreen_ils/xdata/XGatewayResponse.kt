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
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.net.elapsedTime
import net.kenstir.hemlock.net.isCached
import net.kenstir.hemlock.net.debugTag
import net.kenstir.hemlock.net.debugUrl

class XGatewayResponse(val response: HttpResponse) {
    val isCached: Boolean
        get() = response.isCached()

    val elapsed: Long
        get() = response.elapsedTime()

    val debugTag: String
        get() = response.debugTag()

    val debugUrl: String
        get() = response.debugUrl()

    suspend fun bodyAsText(): String {
        val body = response.bodyAsText()
        Analytics.logResponseX(debugTag, debugUrl, isCached, body, elapsed)
        return response.bodyAsText()
    }

    /** given `"payload":[obj]` return `obj` */
    suspend fun payloadFirstAsObject(): XOSRFObject {
        val json = bodyAsText()
        return XGatewayResult.create(json).payloadFirstAsObject()
    }

    /** given `"payload":[obj]` return `obj` or null if payload empty */
    suspend fun payloadFirstAsObjectOrNull(): XOSRFObject? {
        val json = bodyAsText()
        return XGatewayResult.create(json).payloadFirstAsOptionalObject()
    }

    /** given `"payload":[[obj,obj]]` return `[obj,obj]` */
    suspend fun payloadFirstAsObjectList(): List<XOSRFObject> {
        val json = bodyAsText()
        return XGatewayResult.create(json).payloadFirstAsObjectList()
    }

    /** given `"payload":["string"]` return `"string"` */
    suspend fun payloadFirstAsString(): String {
        val json = bodyAsText()
        return XGatewayResult.create(json).payloadFirstAsString()
    }
}
