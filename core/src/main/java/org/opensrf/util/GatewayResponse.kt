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

import android.text.TextUtils
import org.evergreen_ils.Api
import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.utils.fromApiToIntOrNull
import org.open_ils.Event
import org.opensrf.ShouldNotHappenException
import java.util.*
import kotlin.collections.ArrayList

class GatewayResponse {
    enum class ResponseType {
        OBJECT, ARRAY, STRING, EMPTY, UNKNOWN, ERROR
    }

    @JvmField
    var payload: Any? = null
    @JvmField
    var failed = false
    @JvmField
    var description: String? = null
    @JvmField
    var ex: Exception? = null

    //private var map: Map<String?, Any?>? = null
    private var type: ResponseType = ResponseType.UNKNOWN

    private constructor()
    private constructor(ex: Exception) {
        this.ex = ex
        failed = true
        description = ex.message
        type = ResponseType.ERROR
    }

    @Throws(GatewayError::class)
    fun asObject(): OSRFObject {
        return try {
            if (payload is OSRFObject) {
                payload as OSRFObject
            } else  /*if (payload instanceof Map)*/ {
                val map = payload as Map<String, Any>?
                OSRFObject(map)
            }
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected object")
        }
    }

    @Throws(GatewayError::class)
    fun asObjectArray(): List<OSRFObject> {
        return try {
            payload as List<OSRFObject>
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected array")
        }
    }

    @Throws(GatewayError::class)
    fun asString(): String {
        return try {
            if (payload == null) {
                ""
            } else payload as String
        } catch (ex: Exception) {
            throw GatewayError("Unexpected network response: expected string")
        }
    }

    companion object {
        @JvmStatic
        fun create(json: String?): GatewayResponse {
            return try {
                val result = JSONReader(json).readObject()
                        ?: throw GatewayError("Unexpected network response: empty")
                val status = result["status"]?.fromApiToIntOrNull()
                        ?: throw GatewayError("Unexpected network response: missing status")
                if (status != 200)
                    throw GatewayError("Network response failed (status:$status)")
                val responseList= result["payload"] as? List<Any?>?
                        ?: throw GatewayError("Unexpected network response: empty payload")
                val payload = responseList.firstOrNull()
                createFromObject(payload)
            } catch (ex: Exception) {
                GatewayResponse(ex)
            }
        }

        // Really GatewayRequest.recv() should return this directly, but that is used everywhere and
        // I am refactoring incrementally.
        //
        // payload is returned from Utils.doRequest, and AFAIK it can be one of 3 things:
        // 1 - an ilsevent (a map indicating an error or oddly sometimes a success)
        // 2 - a map containing an OSRFObject response
        // 3 - a list of events
        @JvmStatic
        fun createFromObject(payload: Any?): GatewayResponse {
            try {
                val resp = GatewayResponse()
                resp.payload = payload
                when (payload) {
                    null -> {
                        resp.type = ResponseType.EMPTY
                    }
                    is Map<*, *> -> {
                        // object or event
                        val event = Event.parseEvent(payload)
                        if (event != null) {
                            resp.failed = event.failed()
                            resp.description = event.description
                            //if (event.containsKey("payload")) resp.map = event["payload"] as Map<String, *>?
                        }
                        resp.type = ResponseType.OBJECT
                    }
                    is ArrayList<*> -> {
                        // list of objects or list of events
                        val msgs = ArrayList<String?>()
                        for (obj in payload as ArrayList<Any?>) {
                            val event = Event.parseEvent(obj)
                            if (event != null) {
                                if (event.failed()) resp.failed = true
                                msgs.add(event.description)
                            }
                        }
                        resp.description = TextUtils.join("\n\n", msgs)
                        resp.type = ResponseType.ARRAY
                    }
                    is String -> {
                        resp.type = ResponseType.STRING
                    }
                    else -> {
                        resp.type = ResponseType.UNKNOWN
                        resp.failed = true
                        resp.description = "Unexpected network response"
                    }
                }
                return resp
            } catch (ex: Exception) {
                return GatewayResponse(ex)
            }
        }
    }
}