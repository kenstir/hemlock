/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
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

import android.annotation.SuppressLint
import android.content.res.Resources
import net.kenstir.hemlock.R
import net.kenstir.data.JSONDictionary
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.HoldRecord
import org.evergreen_ils.system.EgCodedValueMap.iconFormatLabel
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.util.OSRFUtils
import net.kenstir.util.JsonUtils
import org.evergreen_ils.Api
import org.evergreen_ils.gateway.OSRFObject
import java.text.DateFormat
import java.util.*

class EvergreenHoldRecord(val ahrObj: OSRFObject) : HoldRecord {
    override val id: Int
        get() = ahrObj.getInt("id") ?: 0
    override var record: BibRecord? = null
    var qstatsObj: OSRFObject? = null
    var partLabel: String? = null // only for HOLD_TYPE_PART

    override val transitFrom: String?
        get() {
            val transit = ahrObj["transit"] as? OSRFObject ?: return null
            val source = transit.getInt("source") ?: return null
            return EgOrg.getOrgNameSafe(source)
        }
    override val transitSince: String?
        get() {
            val transit = ahrObj["transit"] as? OSRFObject ?: return null
            val sent = transit.getDate("source_send_time") ?: return null
            return OSRFUtils.formatDateTimeForOutput(sent)
        }

    // Retrieve hold status in text
    @SuppressLint("StringFormatInvalid")
    override fun getHoldStatus(res: Resources): String {
        // Constants from Holds.pm and logic from hold_status.tt2
        // -1 on error (for now),
        //  1 for 'waiting for copy to become available',
        //  2 for 'waiting for copy capture',
        //  3 for 'in transit',
        //  4 for 'arrived',
        //  5 for 'hold-shelf-delay'
        //  6 for 'canceled'
        //  7 for 'suspended'
        //  8 for 'captured, on wrong hold shelf'
        val status = status
        return if (status == null) {
            res.getString(R.string.hold_status_unavailable)
        } else if (status == 4) {
            var s = res.getString(R.string.hold_status_available, pickupOrgName)
            shelfExpireTime?.let {
                if (res.getBoolean(R.bool.ou_enable_hold_shelf_expiration)) {
                    s = s + "\n" + res.getString(R.string.hold_status_expires, DateFormat.getDateInstance().format(it))
                }
            }
            s
        } else if (status == 7) {
            res.getString(R.string.hold_status_suspended)
        } else if (estimatedWaitInSeconds!! > 0) {
            val days = Math.ceil(estimatedWaitInSeconds!!.toDouble() / 86400.0).toInt()
            res.getString(R.string.hold_status_estimated_wait,
                res.getQuantityString(R.plurals.number_of_days, days, days))
        } else if (status == 3 || status == 8) {
            res.getString(R.string.hold_status_in_transit, transitFrom, transitSince)
        } else if (status < 3) {
            var s = res.getString(R.string.hold_status_waiting_for_copy,
                res.getQuantityString(R.plurals.number_of_holds, totalHolds!!, totalHolds),
                res.getQuantityString(R.plurals.number_of_copies, potentialCopies!!, potentialCopies))
            if (res.getBoolean(R.bool.ou_enable_hold_queue_position))
                s = s + "\n" + res.getString(R.string.hold_status_queue_position, queuePosition)
            s
        } else {
            ""
        }
    }

    override val holdType: String
        get() = ahrObj.getString("hold_type", "?")!!

    private fun withPartLabel(title: String): String {
        return if (!partLabel.isNullOrEmpty()) "$title ($partLabel)" else title
    }

    override val title: String
        get() {
            val title = record?.title
            if (!title.isNullOrEmpty())
                return withPartLabel(title)
            return "Unknown Title"
        }
    override val author: String
        get() {
            val author = record?.author
            if (!author.isNullOrEmpty())
                return author
            return ""
        }
    override val expireTime: Date?
        get() = ahrObj.getDate("expire_time")
    override val shelfExpireTime: Date?
        get() = ahrObj.getDate("shelf_expire_time")
    override val thawDate: Date?
        get() = ahrObj.getDate("thaw_date")
    override val target: Int?
        get() = ahrObj.getInt("target")
    override val isEmailNotify: Boolean
        get() = ahrObj.getBoolean("email_notify")
    override val phoneNotify: String?
        get() = ahrObj.getString("phone_notify")
    override val smsNotify: String?
        get() = ahrObj.getString("sms_notify")
    override val isSuspended: Boolean
        get() = ahrObj.getBoolean("frozen")
    override val pickupLib: Int?
        get() = ahrObj.getInt("pickup_lib")
    override val pickupOrgName: String
        get() = EgOrg.getOrgNameSafe(pickupLib)
    val status: Int?
        get() = qstatsObj?.getInt("status")
    val potentialCopies: Int?
        get() = qstatsObj?.getInt("potential_copies")
    val estimatedWaitInSeconds: Int?
        get() = qstatsObj?.getInt("estimated_wait")
    override val queuePosition: Int?
        get() = qstatsObj?.getInt("queue_position")
    override val totalHolds: Int?
        get() = qstatsObj?.getInt("total_holds")
    override val formatLabel: String?
        get() {
            if (holdType == Api.HoldType.METARECORD) {
                val dict = JsonUtils.parseObject(ahrObj.getString("holdable_formats"))
                val codes = parseHoldableFormats(dict)
                val labels = codes.map { iconFormatLabel(it) }
                return labels.joinToString(" or ")
            }
            return record?.iconFormatLabel
        }

    companion object {
        fun makeArray(objects: List<OSRFObject>): ArrayList<EvergreenHoldRecord> {
            val ret = ArrayList<EvergreenHoldRecord>()
            for (obj in objects) {
                ret.add(EvergreenHoldRecord(obj))
            }
            return ret
        }

        /** Parse a metarecord hold attribute "holdable_formats" into a list of ccvm codes */
        fun parseHoldableFormats(dict: JSONDictionary?): ArrayList<String> {
            val formats = ArrayList<String>()
            if (dict == null)
                return formats
            for ((_, v) in dict) {
                val l = v as? ArrayList<*>
                if (l != null) {
                    for (elem in l) {
                        val e = elem as? Map<String, String>
                        val attr = e?.get("_attr")
                        val value = e?.get("_val")
                        if (attr == "mr_hold_format" && value != null) {
                            formats.add(value)
                        }
                    }
                }
            }
            return formats
        }
    }
}
