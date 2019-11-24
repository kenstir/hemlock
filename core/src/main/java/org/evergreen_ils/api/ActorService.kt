/*
 * Copyright (c) 2019 Kenneth H. Cox
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

package org.evergreen_ils.api

import org.evergreen_ils.Api
import org.evergreen_ils.net.Gateway
import org.opensrf.util.OSRFObject

internal const val TAG = "api"

object ActorService {
    suspend fun fetchServerVersion(): String {
        return Gateway.fetchNoCache(Api.ACTOR, Api.ILS_VERSION, arrayOf()) { response ->
            response.asString()
        }
    }

    suspend fun fetchOrgTypes(): List<OSRFObject> {
        return Gateway.fetchObjectArray(Api.ACTOR, Api.ORG_TYPES_RETRIEVE, arrayOf())
    }

    suspend fun fetchOrgTree(): OSRFObject {
        return Gateway.fetchObject(Api.ACTOR, Api.ORG_TREE_RETRIEVE, arrayOf())
    }

    suspend fun fetchOrgSettings(orgID: Int): Any {
        val settings = arrayListOf(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB,
                Api.SETTING_CREDIT_PAYMENTS_ALLOW)
        val args = arrayOf<Any?>(orgID, settings, Api.ANONYMOUS)
        return Gateway.fetch(Api.ACTOR, Api.ORG_UNIT_SETTING_BATCH, args) { response ->
            response.payload
        }
    }

    // map returned from `fetchOrgSettings` looks like:
    // {credit.payments.allow={org=49, value=true}, opac.holds.org_unit_not_pickup_lib=null}
    fun parseBoolSetting(map: Map<String, Any?>, setting: String): Boolean? {
        var value: Boolean? = null
        if (map != null) {
            val o = map[setting]
            if (o != null) {
                val setting_map = o as Map<String, *>
                value = Api.parseBoolean(setting_map["value"])
            }
        }
        return value
    }
}