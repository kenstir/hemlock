/*
 * Copyright (c) 2022 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.data

import android.content.res.Resources
import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.R
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgOrg.getOrgNameSafe
import org.evergreen_ils.utils.MARCRecord
import org.evergreen_ils.utils.MARCXMLParser
import org.evergreen_ils.utils.RecordAttributes
import org.evergreen_ils.utils.TextUtils
import org.opensrf.util.OSRFObject
import java.io.Serializable

class MBRecord(val id: Int, var mvrObj: OSRFObject? = null) : Serializable {
    constructor(mvrObj: OSRFObject) : this(mvrObj.getInt("doc_id") ?: -1, mvrObj)

    var copyCounts: ArrayList<CopyCount>? = null
    @JvmField var marcRecord: MARCRecord? = null
    @JvmField var attrs: HashMap<String, String>? = null
    var isDeleted = false

    @JvmField val hasMetadata = (mvrObj != null)
    @JvmField val title = mvrObj?.getString("title") ?: ""
    val author = mvrObj?.getString("author") ?: ""
    val isbn = mvrObj?.getString("isbn") ?: ""
    @JvmField val online_loc = getFirstOnlineLocation()
    val pubdate = mvrObj?.getString("pubdate") ?: ""
    val physicalDescription = mvrObj?.getString("physical_description") ?: ""
    val synopsis = mvrObj?.getString("synopsis") ?: ""

    val publishingInfo: String
        get() {
            val s = TextUtils.join(" ", arrayOf<String>(
                pubdate,
                mvrObj?.getString("publisher") ?: ""))
            return s.trim()
        }
    val series: String
        get() {
            val seriesList = mvrObj?.get("series") as? List<String?>
            return when (seriesList) {
                null -> ""
                else -> TextUtils.join("\n", seriesList)
            }
        }
    val subject: String
        get() {
            val obj = mvrObj?.getObject("subject") ?: return ""
            return obj.keys.joinToString("\n")
        }

    val iconFormat: String? = attrs?.get("icon_format")
    val iconFormatLabel: String  = EgCodedValueMap.iconFormatLabel(iconFormat) ?: ""

    fun updateFromBREResponse(breObj: OSRFObject) {
        isDeleted = breObj.getBoolean("deleted")
        try {
            val marcxml = breObj.getString("marc")
            if (!TextUtils.isEmpty(marcxml)) {
                val parser = MARCXMLParser(marcxml)
                marcRecord = parser.parse()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun updateFromMRAResponse(mraObj: OSRFObject?) {
        attrs = RecordAttributes.parseAttributes(mraObj)
    }

    fun getAttr(attrName: String?): String? {
        return attrs?.get(attrName)
    }

    fun getCopySummary(resources: Resources, orgID: Int?): String {
        var total = 0
        var available = 0
        if (copyCounts == null) return ""
        if (orgID == null) return ""
        for (i in copyCounts!!.indices) {
            if (copyCounts!![i].orgId == orgID) {
                total = copyCounts!![i].count!!
                available = copyCounts!![i].available!!
                break
            }
        }
        val totalCopies = resources.getQuantityString(R.plurals.number_of_copies, total, total)
        return String.format(resources.getString(R.string.n_of_m_available),
            available, totalCopies, getOrgNameSafe(orgID))
    }

    fun getFirstOnlineLocation(): String? {
        val l = mvrObj?.get("online_loc") as? List<*> ?: return null
        return when(l.size) {
            0 -> null
            else -> l[0].toString()
        }
    }

    companion object {
        /** Create array of skeleton records from the multiclass.query response obj.
         * The "ids" field is a list of lists and looks like one of:
         * [[32673,null,"0.0"],[886843,null,"0.0"]]      // integer id,?,?
         * [["503610",null,"0.0"],["502717",null,"0.0"]] // string id,?,?
         * [["1805532"],["2385399"]]                     // string id only
         */
        fun makeArray(idsList: List<List<*>>): ArrayList<MBRecord> {
            val records = ArrayList<MBRecord>()
            for (i in idsList.indices) {
                val id = OSRFUtils.parseInt(idsList[i][0])
                records.add(MBRecord(id))
            }
            return records
        }
    }
}
