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
package org.opensrf.util

import org.evergreen_ils.data.JSONDictionary
import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.net.GatewayEventError
import org.evergreen_ils.utils.fromApiToIntOrNull
import org.open_ils.Event

class GatewayResult {
    enum class ResultType {
        OBJECT, ARRAY, STRING, EMPTY, EVENT, UNKNOWN, ERROR
    }

    @JvmField
    var payload: Any? = null
    @JvmField
    var failed = false
    @JvmField
    var errorMessage: String? = null

    private var error: GatewayError? = null
    private var events: List<Event>? = null
    private var type: ResultType = ResultType.UNKNOWN

    private constructor()
    private constructor(error: GatewayError) {
        this.error = error
        failed = true
        errorMessage = error.message
        type = ResultType.ERROR
    }
    private constructor(ex: Exception): this(GatewayError(ex))

    @Throws(GatewayError::class)
    fun asObject(): OSRFObject {
        error?.let { throw it }
        try {
            (payload as? OSRFObject)?.let { return it }
            (payload as? JSONDictionary)?.let { return OSRFObject(it) }
            throw GatewayError("Unexpected type")
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected object, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asOptionalObject(): OSRFObject? {
        error?.let { throw it }
        return try {
            payload as? OSRFObject
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected object, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asMap(): JSONDictionary {
        error?.let { throw it }
        return try {
            payload as JSONDictionary
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected map, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asObjectArray(): List<OSRFObject> {
        error?.let { throw it }
        return try {
            payload as List<OSRFObject>
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected array, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asArray(): List<Any> {
        error?.let { throw it }
        return try {
            payload as List<Any>
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected array, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asString(): String {
        error?.let { throw it }
        return try {
            payload as String
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected string, got $type")
        }
    }

    companion object {
        @JvmStatic
        fun create(json: String): GatewayResult {
            return try {
                val result = JSONReader(json).readObject()
                        ?: throw GatewayError("Unexpected network response: empty")
                val status = result["status"]?.fromApiToIntOrNull()
                        ?: throw GatewayError("Unexpected network response: missing status")
                if (status != 200)
                    throw GatewayError("Network request failed (status:$status)")
                val responseList= result["payload"] as? List<Any?>?
                        ?: throw GatewayError("Unexpected network response: missing payload")
                val payload = responseList.firstOrNull()
                createFromPayload(payload)
            } catch (ex: JSONParserException) {
                if (json?.contains("canceling statement due to user request")) {
                    GatewayResult(GatewayError("Timeout; the request took too long to complete and the server killed it"))
                } else {
                    GatewayResult(GatewayError("Internal Server Error; the server response is not JSON"))
                }
            } catch (ex: Exception) {
                GatewayResult(ex)
            }
        }

        @JvmStatic
        fun createFromPayload(payload: Any?): GatewayResult {
            try {
                val resp = GatewayResult()
                resp.payload = payload
                when (payload) {
                    null -> {
                        resp.type = ResultType.EMPTY
                    }
                    is Map<*, *> -> {
                        // object or event
                        val event = Event.parseEvent(payload)
                        if (event != null) {
                            resp.failed = event.failed()
                            if (resp.failed) {
                                resp.errorMessage = event.message
                                resp.error = GatewayEventError(event)
                            }
                            resp.events = listOf(event)
                            resp.type = ResultType.EVENT
                        } else {
                            resp.type = ResultType.OBJECT
                        }
                    }
                    is ArrayList<*> -> {
                        // list of objects or list of events
                        val obj = payload.firstOrNull()
                        val event = Event.parseEvent(obj)
                        if (event != null && event.failed()) {
                            resp.failed = true
                            resp.errorMessage = event.message
                            resp.error = GatewayEventError(event)
                            resp.events = listOf(event)
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
                        resp.failed = true
                        resp.errorMessage = "Unexpected network response"
                    }
                }
                return resp
            } catch (ex: Exception) {
                return GatewayResult(ex)
            }
        }
    }
}
