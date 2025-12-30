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
package org.evergreen_ils.util

import android.annotation.SuppressLint
import net.kenstir.data.ShouldNotHappenException
import net.kenstir.logging.Log
import net.kenstir.util.Analytics.logException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OSRFUtils {
    const val API_DATE_PATTERN: String = "yyyy-MM-dd'T'HH:mm:ssZ"
    const val API_DAY_ONLY_PATTERN: String = "yyyy-MM-dd"
    const val API_HOURS_PATTERN: String = "HH:mm:ss"
    const val OUTPUT_DATE_PATTERN: String = "MM/dd/yyyy"
    const val OUTPUT_DATE_TIME_PATTERN: String = "MM/dd/yyyy h:mm a"

    /** format ISO date+time string to pass to API methods  */
    fun formatDate(date: Date): String {
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(API_DATE_PATTERN)
        return df.format(date)
    }

    /** format ISO date string yyyy-MM-dd to pass to API methods  */
    fun formatDateAsDayOnly(date: Date): String {
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(API_DAY_ONLY_PATTERN)
        return df.format(date)
    }

    /** parse ISO date+time string returned from API methods  */
    fun parseDate(dateString: String?): Date? {
        //Log.d("Utils", "[dbg] parseDate: dateString=$dateString")
        if (dateString == null || dateString.isEmpty()) return null

        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(API_DATE_PATTERN)

        try {
            return df.parse(dateString)
        } catch (e: ParseException) {
            logException(e)
            return null
        }
    }

    /** parse time string HH:MM:SS returned from API  */
    fun parseHours(hoursString: String?): Date? {
        //Log.d("Utils", "[dbg] parseHours: hoursString=$hoursString")
        if (hoursString == null || hoursString.isEmpty()) return null

        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(API_HOURS_PATTERN)

        try {
            return df.parse(hoursString)
        } catch (e: ParseException) {
            return null
        }
    }

    fun formatHoursForOutput(date: Date): String {
        // Use the default locale instead of fixed AM/PM, even though
        // this will make the tests break when run in a non-US locale.
        val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        return timeFormatter.format(date)
    }

    fun formatDateForOutput(date: Date): String {
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(OUTPUT_DATE_PATTERN)
        return df.format(date)
    }

    fun formatDateTimeForOutput(date: Date): String {
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat(OUTPUT_DATE_TIME_PATTERN)
        return df.format(date)
    }

    /** Parses bool from string returned from API methods
     */
    fun parseBoolean(obj: Any?): Boolean {
        //Log.d("Utils", "[dbg] parseBoolean: obj=$obj")
        if (obj is Boolean) {
            return obj
        } else if (obj is String) {
            val s = obj
            return s == "t"
        } else {
            return false
        }
    }

    /**
     * Returns o as an Integer
     *
     *
     * Sometimes search returns a count as a json number ("count":0), sometimes a string ("count":"1103").
     * Seems to be the same for result "ids" list (See Issue #1).  Handle either form and return as an int.
     */
    @JvmOverloads
    fun parseInt(o: Any?, defaultValue: Int? = null): Int? {
        if (o == null) {
            return defaultValue
        } else if (o is Int) {
            return o
        } else if (o is String) {
            // I have seen settings with value "", e.g. opac.default_sms_carrier
            return try {
                o.toInt()
            } catch (e: NumberFormatException) {
                defaultValue
            }
        } else {
            logException(ShouldNotHappenException("unexpected type: $o isa ${o::class.simpleName}"))
            return defaultValue
        }
    }

    fun parseIdsListAsInt(o: Any?): List<Int> {
        //Log.d("Utils", "[dbg] parseIdsListAsInt: o=$o")
        val ret = mutableListOf<Int>()
        if (o is List<*>) {
            for (elem in o) {
                val i: Int? = parseInt(elem)
                if (i != null) {
                    ret.add(i)
                }
            }
        }
        return ret
    }
}
