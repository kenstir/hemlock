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

package org.evergreen_ils.gateway

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.discard
import io.ktor.utils.io.readAvailable
import net.kenstir.util.Analytics
import net.kenstir.data.elapsedTime
import net.kenstir.data.isCached
import net.kenstir.data.debugTag
import net.kenstir.data.debugUrl

class GatewayResponse(val response: HttpResponse) {
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

    suspend fun discardResponseBody() {
        // read a small amount of the body for logging purposes, discard the rest
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(Analytics.MAX_DATA_SHOWN)
        val bytesRead = channel.readAvailable(buffer, 0, Analytics.MAX_DATA_SHOWN)
        channel.discard()
        val str = if (bytesRead > 0) {
            String(buffer, 0, bytesRead).trim()
        } else {
            ""
        }
        Analytics.logResponseX(debugTag, debugUrl, isCached, str, elapsed)

        // to simply discard everything
//        response.bodyAsChannel().discard()
//        Analytics.logResponseX(debugTag, debugUrl, isCached, "(discarded)", elapsed)
    }

    /** given `"payload":["string"]` return `"string"` */
    suspend fun payloadFirstAsString(): String {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadFirstAsString()
    }

    /** given `"payload":[obj]` return `obj` */
    suspend fun payloadFirstAsObject(): OSRFObject {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadFirstAsObject()
    }

    /** given `"payload":[obj]` return `obj` or null if payload empty */
    suspend fun payloadFirstAsObjectOrNull(): OSRFObject? {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadFirstAsOptionalObject()
    }

    /** given `"payload":[obj,obj]` return `[obj,obj]` */
    suspend fun payloadAsObjectList(): List<OSRFObject> {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadAsObjectList()
    }

    /** given `"payload":[[obj,obj]]` return `[obj,obj]` */
    suspend fun payloadFirstAsObjectList(): List<OSRFObject> {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadFirstAsObjectList()
    }

    /** given `"payload":[[any]]` return `[any]` */
    suspend fun payloadFirstAsList(): List<Any> {
        val json = bodyAsText()
        return GatewayResult.create(json).payloadFirstAsList()
    }
}
