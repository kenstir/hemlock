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
    //@JvmField
    private var ex: Exception? = null
    private var events: List<Event>? = null

    private var type: ResultType = ResultType.UNKNOWN

    private constructor()
    private constructor(ex: Exception) {
        this.ex = ex
        failed = true
        errorMessage = ex.message
        type = ResultType.ERROR
    }

    @Throws(GatewayError::class)
    fun asObject(): OSRFObject {
        return try {
            payload as OSRFObject
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected object, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asOptionalObject(): OSRFObject? {
        return try {
            payload as? OSRFObject
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected object, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asMap(): JSONDictionary {
        return try {
            payload as JSONDictionary
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected map, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asObjectArray(): List<OSRFObject> {
        return try {
            payload as List<OSRFObject>
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected array, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asArray(): List<Any> {
        return try {
            payload as List<Any>
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected array, got $type")
        }
    }

    @Throws(GatewayError::class)
    fun asString(): String {
        return try {
            payload as String
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected string, got $type")
        }
    }

    companion object {
        @JvmStatic
        fun create(json: String?): GatewayResult {
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
                createFromObject(payload)
            } catch (ex: Exception) {
                GatewayResult(ex)
            }
        }

        // Really GatewayRequest.recv() should return this directly, but that is used everywhere and
        // I am refactoring incrementally.
        //
        // payload is returned from Utils.doRequest, and AFAIK it can be one of 3 things:
        // 1 - an ilsevent (a map indicating an error or oddly sometimes a success)
        // 2 - a map containing an OSRFObject response
        // 3 - a list of events or OSRFObjects
        @JvmStatic
        fun createFromObject(payload: Any?): GatewayResult {
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
                            if (resp.failed) resp.errorMessage = event.description
                            resp.events = listOf(event)
                            resp.type = ResultType.EVENT
                        } else {
                            resp.type = ResultType.OBJECT
                        }
                    }
                    is ArrayList<*> -> {
                        // list of objects or list of events
                        val events = mutableListOf<Event>()
                        for (obj in payload as ArrayList<Any?>) {
                            Event.parseEvent(obj)?.let { event ->
                                if (event.failed()) resp.failed = true
                                events.add(event)
                            }
                        }
                        if (resp.failed) resp.errorMessage = events.joinToString("\n\n") { it.description }
                        resp.type = ResultType.ARRAY
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
