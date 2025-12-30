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
package org.evergreen_ils.data.model

import android.text.TextUtils
import net.kenstir.data.model.Link
import java.io.Serializable

// TODO: remove Serializable, that will make the TransactionTooLargeException issue worse
class MARCRecord : Serializable {
    class MARCSubfield(var code: String?) : Serializable {
        @JvmField
        var text: String? = null
    }

    class MARCDatafield(var tag: String?, var ind1: String?, var ind2: String?) : Serializable {
        @JvmField
        var subfields: MutableList<MARCSubfield> = ArrayList()

        val isOnlineLocation: Boolean
            get() = isOnlineLocation(tag, ind1, ind2)

        val isTitleStatement: Boolean
            get() = isTitleStatement(tag)

        val nonFilingCharacters: Int?
            // only valid if isTitleStatement
            get() {
                return ind2?.toIntOrNull()
            }

        val uri: String?
            get() {
                for (sf in subfields) {
                    if (TextUtils.equals(sf.code, "u"))
                        return sf.text
                }
                return null
            }

        val linkText: String?
            get() {
                for (sf in subfields) {
                    if (TextUtils.equals(sf.code, "y"))
                        return sf.text
                }
                for (sf in subfields) {
                    if (TextUtils.equals(sf.code, "z") || TextUtils.equals(sf.code, "3"))
                        return sf.text
                }
                return "Tap to access"
            }
    }

    @JvmField
    var datafields: MutableList<MARCDatafield> = ArrayList<MARCDatafield>()

    val links: MutableList<Link>
        get() {
            val links = ArrayList<Link>()
            for (df in datafields) {
                if (df.isOnlineLocation) {
                    var href: String? = null
                    var text: String? = null
                    for (sf in df.subfields) {
                        if (TextUtils.equals(sf.code, "u") && href == null)
                            href = sf.text
                        if ((TextUtils.equals(sf.code, "3") || TextUtils.equals(sf.code, "y")) && text == null)
                            text = sf.text
                    }
                    if (href != null && text != null) {
                        links.add(Link(href, text))
                    }
                }
            }
            return links
        }

    companion object {
        fun isOnlineLocation(tag: String?, ind1: String?, ind2: String?): Boolean {
            return (TextUtils.equals(tag, "856")
                    && TextUtils.equals(ind1, "4")
                    && (TextUtils.equals(ind2, "0")
                    || TextUtils.equals(ind2, "1")
                    || TextUtils.equals(ind2, "2")))
        }

        fun isTitleStatement(tag: String?): Boolean {
            return (TextUtils.equals(tag, "245"))
        }

        @JvmStatic
        fun isDatafieldUseful(tag: String?, ind1: String?, ind2: String?): Boolean {
            return isOnlineLocation(tag, ind1, ind2) || isTitleStatement(tag)
        }
    }
}
