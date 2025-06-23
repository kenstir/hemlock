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
package org.evergreen_ils.system

import net.kenstir.hemlock.data.evergreen.OSRFUtils
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.data.ShouldNotHappenException
import net.kenstir.hemlock.data.evergreen.XOSRFObject
import java.util.*

object EgCodedValueMap {
    private val TAG = EgCodedValueMap::class.java.simpleName

    const val SEARCH_FORMAT = "search_format"
    const val ICON_FORMAT = "icon_format"
    const val ALL_SEARCH_FORMATS = "All Formats"

    internal data class CodedValue(val code: String, val value: String, val opacVisible: Boolean)

    private var searchFormats = ArrayList<CodedValue>()
    private var iconFormats = ArrayList<CodedValue>()

    @JvmStatic
    fun loadCodedValueMaps(objects: List<XOSRFObject>) {
        searchFormats = ArrayList()
        iconFormats = ArrayList()
        for (obj in objects) {
            val ctype = obj.getString("ctype", "")
            val code = obj.getString("code") ?: continue
            val opac_visible = OSRFUtils.parseBoolean(obj["opac_visible"])
            val search_label = obj.getString("search_label") ?: ""
            val value = obj.getString("value") ?: ""
            val cv = CodedValue(code, if (search_label.isNotBlank()) search_label else value, opac_visible)
            Log.d(TAG, "ccvm ctype:" + ctype + " code:" + code + " label:" + cv.value)
            if (ctype == SEARCH_FORMAT) {
                searchFormats.add(cv)
            } else if (ctype == ICON_FORMAT) {
                iconFormats.add(cv)
            }
        }
    }

    fun getValueFromCode(ctype: String, code: String?): String? {
        val codedValues: ArrayList<CodedValue> = when (ctype) {
            SEARCH_FORMAT -> searchFormats
            ICON_FORMAT -> iconFormats
            else -> return null
        }
        val cv = codedValues.firstOrNull { code == it.code }
        // It happens often...~4k times in 30d for PINES
        // and represents a cataloging problem, not an app issue
        //if (cv == null) Analytics.logException(ShouldNotHappenException("Unknown ccvm code: $code"))
        return cv?.value
    }

    fun getCodeFromValue(ctype: String, value: String?): String? {
        val codedValues: ArrayList<CodedValue> = when (ctype) {
            SEARCH_FORMAT -> searchFormats
            ICON_FORMAT -> iconFormats
            else -> return null
        }
        val cv = codedValues.firstOrNull { value == it.value }
        if (cv == null)
            Analytics.logException(ShouldNotHappenException("Unknown ccvm value: $value"))
        return cv?.code
    }

    @JvmStatic
    fun iconFormatLabel(code: String?): String? {
        return getValueFromCode(ICON_FORMAT, code)
    }

    @JvmStatic
    fun searchFormatLabel(code: String): String? {
        return getValueFromCode(SEARCH_FORMAT, code)
    }

    @JvmStatic
    fun searchFormatCode(label: String?): String? {
        return if (label.isNullOrBlank() || label == ALL_SEARCH_FORMATS) "" else getCodeFromValue(SEARCH_FORMAT, label)
    }

    /// list of labels e.g. "All Formats", "All Books", ...
    @JvmStatic
    val searchFormatSpinnerLabels: List<String>
        get() {
            val labels = ArrayList<String>()
            for (cv in searchFormats) {
                if (cv.opacVisible) {
                    labels.add(cv.value)
                }
            }
            labels.sort()
            labels.add(0, ALL_SEARCH_FORMATS)
            return labels
        }
}
