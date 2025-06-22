/*
 * Copyright (C) 2019 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package net.kenstir.hemlock.data.evergreen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import net.kenstir.hemlock.data.JSONDictionary

class XGatewayResult {
    enum class ResultType {
        STRING, OBJECT, ARRAY, EMPTY, EVENT, UNKNOWN, ERROR
    }

    @JvmField
    var payload: List<Any?> = ArrayList()
    @JvmField
    var failed = false
    @JvmField
    var errorMessage: String? = null

    // don't cache failures or empty results
    // (empty results can be caused by timeouts or server errors)
    val shouldCache: Boolean
        get() = (!failed && type != ResultType.EMPTY)

    private var error: GatewayError? = null
    private var type: ResultType = ResultType.UNKNOWN

    private constructor()
    private constructor(error: GatewayError) {
        this.error = error
        failed = true
        errorMessage = error.message
        type = ResultType.ERROR
    }
    private constructor(ex: Exception): this(GatewayError(ex))

    /** given `"payload":["string"]` return `"string"` */
    @Throws(GatewayError::class)
    fun payloadFirstAsString(): String {
        error?.let { throw it }
        return try {
            payload.first() as String
        } catch (ex: Exception) {
            throw GatewayError("Internal Server Error: expected string, got $type")
        }
    }

    /** given `"payload":[obj]` return `obj` */
    @Throws(GatewayError::class)
    fun payloadFirstAsObject(): XOSRFObject {
        error?.let { throw it }
        try {
            (payload.firstOrNull() as? XOSRFObject)?.let { return it }
            (payload.firstOrNull() as? JSONDictionary)?.let { return XOSRFObject(it) } // NOT HIT
            throw GatewayError("Unexpected type")
        } catch (ex: Exception) {
            throw GatewayError("Internal Server Error: expected object, got $type")
        }
    }

    /** given `"payload":[obj]` return `obj` or null if payload empty */
    @Throws(GatewayError::class)
    fun payloadFirstAsOptionalObject(): XOSRFObject? {
        return payloadAsObjectList().firstOrNull()
    }

    /** given `"payload":[obj,obj]` return `[obj,obj]` */
    @Throws(GatewayError::class)
    fun payloadAsObjectList(): List<XOSRFObject> {
        error?.let { throw it }
        return try {
            when (type) {
                ResultType.EMPTY -> ArrayList()
                else -> payload as List<XOSRFObject>

            }
        } catch (ex: Exception) {
            throw GatewayError("Internal Server Error: expected array, got $type")
        }
    }

    /** given `"payload":[[obj,obj]]` return `[obj,obj]` */
    @Throws(GatewayError::class)
    fun payloadFirstAsObjectList(): List<XOSRFObject> {
        error?.let { throw it }
        return try {
            val first = payload.firstOrNull() as List<Any>
            first as List<XOSRFObject>
        } catch (ex: Exception) {
            throw GatewayError("Internal Server Error: expected array, got $type")
        }
    }

    /** given `"payload":[[any]]` return `[any]` */
    @Throws(GatewayError::class)
    fun payloadFirstAsList(): List<Any> {
        error?.let { throw it }
        return try {
            val inner = payload as List<Any>
            inner.first() as List<Any>
        } catch (ex: Exception) {
            throw GatewayError("Internal Server Error: expected array, got $type")
        }
    }

    companion object {
        /** Create a XGatewayResult from [json] returned from the OSRF gateway */
        @JvmStatic
        fun create(json: String): XGatewayResult {
            return try {
                val response = Json.decodeFromString<XGatewayResponseContent>(json)
                if (response.status != 200)
                    throw GatewayError("Request failed with status ${response.status}")
                createFromPayload(response.payload)
            } catch (ex: SerializationException) {
                XGatewayResult(GatewayError("Internal Server Error: response is not JSON"))
            } catch (ex: Exception) {
                XGatewayResult(ex)
            }
        }

        @JvmStatic
        fun createFromPayload(encodedPayload: JsonArray): XGatewayResult {
            try {
                val resp = XGatewayResult()
                resp.payload = XOSRFCoder.decodePayload(encodedPayload)

                when (val first = resp.payload.firstOrNull()) {
                    null -> {
                        resp.type = ResultType.EMPTY
                    }
                    is XOSRFObject -> {
                        // object or event
                        val event = Event.parseEvent(first)
                        if (event != null) {
                            resp.failed = true
                            resp.errorMessage = event.message
                            resp.error = GatewayEventError(event)
                            resp.type = ResultType.EVENT
                        } else {
                            resp.type = ResultType.OBJECT
                        }
                    }
                    is List<*> -> {
                        // list of objects or list of events
                        val obj = first.firstOrNull()
                        val event = Event.parseEvent(obj)
                        if (event != null) {
                            resp.failed = true
                            resp.errorMessage = event.message
                            resp.error = GatewayEventError(event)
                            resp.type = ResultType.EVENT
                        } else {
                            resp.type = ResultType.ARRAY
                        }
                    }
                    is String -> {
                        resp.type = ResultType.STRING
                    }
                    else -> {
                        resp.type = ResultType.UNKNOWN
                    }
                }
                return resp
            } catch (ex: Exception) {
                return XGatewayResult(ex)
            }
        }
    }
}
