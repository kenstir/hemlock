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
import net.kenstir.hemlock.R
import net.kenstir.hemlock.data.model.BibRecord
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgOrg.getOrgNameSafe
import org.evergreen_ils.utils.MARCRecord
import org.evergreen_ils.utils.MARCXMLParser
import org.evergreen_ils.utils.RecordAttributes
import org.evergreen_ils.utils.TextUtils
import net.kenstir.hemlock.util.titleSortKey
import org.evergreen_ils.xdata.XOSRFObject
import org.opensrf.util.OSRFObject

class MBRecord(override val id: Int, var mvrObj: XOSRFObject? = null): BibRecord {
    constructor(mvrObj: XOSRFObject) : this(mvrObj.getInt("doc_id") ?: -1, mvrObj)
    constructor(ogObj: OSRFObject) : this(ogObj.getInt("doc_id") ?: -1) {
        TODO("MBRecord constructor from OSRFObject not implemented")
    }

    var copyCounts: ArrayList<CopyCount>? = null
    override var marcRecord: MARCRecord? = null
    var attrs: HashMap<String, String>? = null
    override var isDeleted = false

    override val author: String
        get() = mvrObj?.getString("author") ?: ""
    override val isbn: String
        get() = mvrObj?.getString("isbn") ?: ""
    override val pubdate: String
        get() = mvrObj?.getString("pubdate") ?: ""
    override val description: String
        get() = mvrObj?.getString("physical_description") ?: ""
    override val synopsis: String
        get() = mvrObj?.getString("synopsis") ?: ""
    override val title: String
        get() = mvrObj?.getString("title") ?: ""
    override val titleSortKey: String
        get() {
            if (hasMarc()) {
                val skip = nonFilingCharacters
                if (skip != null && skip > 0) {
                    return title.uppercase().substring(skip).trim()
                }
                return title.uppercase().replace("^[^A-Z0-9]*".toRegex(), "")
            } else {
                return titleSortKey(title) ?: ""
            }
        }
    override val nonFilingCharacters: Int?
        get() {
            marcRecord?.let {
                for (df in it.datafields) {
                    if (df.isTitleStatement) {
                        df.nonFilingCharacters?.let { n ->
                            return n
                        }
                    }
                }
            }
            return null
        }

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
            return obj.map.keys.joinToString("\n")
        }

    val iconFormat: String?
        get() = attrs?.get("icon_format")
    val iconFormatLabel: String
        get() = EgCodedValueMap.iconFormatLabel(iconFormat) ?: ""

    override fun hasAttributes() = (attrs != null)
    override fun hasMarc() = (marcRecord != null)
    override fun hasMetadata() = (mvrObj != null)

    fun updateFromBREResponse(breObj: XOSRFObject) {
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

    fun updateFromMRAResponse(mraObj: XOSRFObject?) {
        attrs = RecordAttributes.parseAttributes(mraObj)
    }

    override fun getAttr(attrName: String?): String? {
        return attrs?.get(attrName)
    }

    fun getCopySummary(resources: Resources, orgID: Int?): String {
        var total = 0
        var available = 0
        if (copyCounts == null) return ""
        if (orgID == null) return ""
        for (copyCount in copyCounts.orEmpty()) {
            if (copyCount.orgId == orgID) {
                total = copyCount.count
                available = copyCount.available
                break
            }
        }
        val totalCopies = resources.getQuantityString(R.plurals.number_of_copies, total, total)
        return String.format(resources.getString(R.string.n_of_m_available),
            available, totalCopies, getOrgNameSafe(orgID))
    }

    fun totalCopies(orgID: Int?): Int {
        for (copyCount in copyCounts.orEmpty()) {
            if (copyCount.orgId == orgID) {
                return copyCount.count
            }
        }
        return 0
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
            val records = ArrayList<MBRecord>(idsList.size)
            for (i in idsList.indices) {
                OSRFUtils.parseInt(idsList[i][0])?.let { id ->
                    records.add(MBRecord(id))
                }
            }
            return records
        }
    }
}
