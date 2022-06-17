/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.data

import android.content.res.Resources
import org.evergreen_ils.HOLD_TYPE_METARECORD
import org.evergreen_ils.OSRFUtils
import org.evergreen_ils.R
import org.evergreen_ils.utils.JsonUtils.parseObject
import org.evergreen_ils.utils.JsonUtils.parseHoldableFormats
import org.evergreen_ils.system.EgCodedValueMap.iconFormatLabel
import org.opensrf.util.OSRFObject
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.TextUtils
import java.io.Serializable
import java.text.DateFormat
import java.util.*

private val TAG = HoldRecord::class.java.simpleName

class HoldRecord(val ahr: OSRFObject) : Serializable {
    var record: MBRecord? = null
    var qstatsObj: OSRFObject? = null
    var partLabel: String? = null // only for HOLD_TYPE_PART

    private fun formatDateTime(date: Date): String {
        return DateFormat.getDateTimeInstance().format(date)
    }

    private val transitFrom: String?
        private get() {
            val transit = ahr["transit"] as? OSRFObject ?: return null
            val source = transit.getInt("source") ?: return null
            return EgOrg.getOrgNameSafe(source)
        }
    private val transitSince: String?
        private get() {
            val transit = ahr["transit"] as OSRFObject ?: return null
            val sent = transit.getString("source_send_time")
            val date = OSRFUtils.parseDate(sent)
            return formatDateTime(date)
        }

    // Retrieve hold status in text
    fun getHoldStatus(res: Resources): String {
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
            "Status unavailable"
        } else if (status == 4) {
            var s = "Available"
            if (res.getBoolean(R.bool.ou_enable_hold_shelf_expiration) && shelfExpireTime != null) s =
                "$s\nExpires " + DateFormat.getDateInstance().format(
                    shelfExpireTime
                )
            s
        } else if (status == 7) {
            "Suspended"
        } else if (estimatedWaitInSeconds!! > 0) {
            val days = Math.ceil(estimatedWaitInSeconds!!.toDouble() / 86400.0).toInt()
            ("Estimated wait: "
                    + res.getQuantityString(R.plurals.number_of_days, days, days))
        } else if (status == 3 || status == 8) {
            res.getString(R.string.hold_status_in_transit, transitFrom, transitSince)
        } else if (status < 3) {
            var s = """Waiting for copy
${res.getQuantityString(R.plurals.number_of_holds, totalHolds!!, totalHolds)} on ${
                res.getQuantityString(
                    R.plurals.number_of_copies, potentialCopies!!, potentialCopies
                )
            }"""
            if (res.getBoolean(R.bool.ou_enable_hold_queue_position)) s =
                "$s\nQueue position: $queuePosition"
            s
        } else {
            ""
        }
    }

    val holdType: String
        get() = ahr.getString("hold_type", "?")!!

    private fun withPartLabel(title: String): String {
        return if (!TextUtils.isEmpty(partLabel)) "$title ($partLabel)" else title
    }

    val title: String
        get() = if (record != null && !TextUtils.isEmpty(record!!.title)) withPartLabel(
            record!!.title
        ) else "Unknown title"
    val author: String
        get() = if (record != null && !TextUtils.isEmpty(record!!.author)) record!!.author else ""
    val expireTime: Date?
        get() = ahr.getDate("expire_time")
    val shelfExpireTime: Date?
        get() = ahr.getDate("shelf_expire_time")
    val thawDate: Date?
        get() = ahr.getDate("thaw_date")
    val target: Int?
        get() = ahr.getInt("target")
    val isEmailNotify: Boolean
        get() = ahr.getBoolean("email_notify")
    val phoneNotify: String?
        get() = ahr.getString("phone_notify")
    val smsNotify: String?
        get() = ahr.getString("sms_notify")
    val isSuspended: Boolean
        get() = ahr.getBoolean("frozen")
    val pickupLib: Int?
        get() = ahr.getInt("pickup_lib")
    val status: Int?
        get() = qstatsObj?.getInt("status")
    val potentialCopies: Int?
        get() = qstatsObj?.getInt("potential_copies")
    val estimatedWaitInSeconds: Int?
        get() = qstatsObj?.getInt("estimated_wait")
    val queuePosition: Int?
        get() = qstatsObj?.getInt("queue_position")
    val totalHolds: Int?
        get() = qstatsObj?.getInt("total_holds")
    val formatLabel: String?
        get() {
            if (holdType == HOLD_TYPE_METARECORD) {
                val map = parseObject(
                    ahr.getString("holdable_formats")
                )
                val labels: MutableList<String?> = ArrayList()
                for (format in parseHoldableFormats(map)) {
                    labels.add(iconFormatLabel(format))
                }
                return android.text.TextUtils.join(" or ", labels)
            }
            return record?.iconFormatLabel
        }

    companion object {
        fun makeArray(ahr_objects: List<OSRFObject>): ArrayList<HoldRecord> {
            val ret = ArrayList<HoldRecord>()
            for (ahr_obj in ahr_objects) {
                ret.add(HoldRecord(ahr_obj))
            }
            return ret
        }
    }
}
