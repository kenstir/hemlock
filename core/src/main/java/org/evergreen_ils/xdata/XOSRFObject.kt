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

import java.util.Date
import kotlinx.serialization.Serializable
import org.evergreen_ils.data.OSRFUtils

@Serializable(with = XOSRFObjectSerializer::class)
data class XOSRFObject(
    val map: Map<String, Any?> = emptyMap(),
    val netClass: String? = null)
{
    override fun toString(): String {
        return "XOSRFObject(netClass=$netClass, map${map.toString()})"
    }

    operator fun get(key: String): Any? {
        return map[key]
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return map[key] as? String ?: defaultValue
    }

    fun getInt(key: String): Int? {
        return when (val value = map[key]) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    fun getBoolean(key: String): Boolean {
        return when (val value = map[key]) {
            is Boolean -> value
            is String -> value == "t"
            else -> false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getObject(key: String): XOSRFObject? {
        return when (val value = map[key]) {
            is XOSRFObject -> value
            is Map<*, *> -> XOSRFObject(value as Map<String, Any?>)
            else -> null
        }
    }

    fun getDate(key: String): Date? {
        return OSRFUtils.parseDate(getString(key))
    }

    fun getAny(key: String): Any? {
        return map[key]
    }

    /**
     * get the boolean value for [setting] from an org settings object `{"credit.payments.allow":{"org":49, "value":true}}`
     */
    fun getBooleanValueFromOrgSetting(setting: String): Boolean? {
        val valueObj = getObject(setting)
        return valueObj?.getBoolean("value")
    }

    /**
     * get the string value for [setting] from an org settings object `{"lib.info_url":{"org":49, "value":"https://example.com"}}`
     */
    fun getStringValueFromOrgSetting(setting: String): String? {
        val valueObj = getObject(setting)
        return valueObj?.getString("value")
    }
}
