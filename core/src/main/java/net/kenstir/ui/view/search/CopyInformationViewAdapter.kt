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

package net.kenstir.ui.view.search

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.CopyLocationCounts
import net.kenstir.hemlock.R
import net.kenstir.ui.Key
import net.kenstir.ui.view.OrgDetailsActivity
import org.evergreen_ils.system.EgOrg

class CopyInformationViewAdapter(
    private val items: List<CopyLocationCounts>,
    private val groupCopiesBySystem: Boolean,
) : RecyclerView.Adapter<CopyInformationViewAdapter.ViewHolder>() {

    class ViewHolder(v: View, val groupCopiesBySystem: Boolean) : RecyclerView.ViewHolder(v) {
        private val majorLocationText = v.findViewById<TextView>(R.id.copy_information_major_location)
        private val minorLocationText = v.findViewById<TextView>(R.id.copy_information_minor_location)
        private val copyCallNumberText = v.findViewById<TextView>(R.id.copy_information_call_number)
        private val copyLocationText = v.findViewById<TextView>(R.id.copy_information_copy_location)
        private val copyStatusesText = v.findViewById<TextView>(R.id.copy_information_statuses)

        fun bindView(clc: CopyLocationCounts) {
            val org = EgOrg.findOrg(clc.orgId)

            if (groupCopiesBySystem) {
                majorLocationText.text = EgOrg.getOrgNameSafe(org?.parent)
                val ss = SpannableString(org?.name)
                ss.setSpan(URLSpan(""), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                minorLocationText.setText(ss, TextView.BufferType.SPANNABLE)
                minorLocationText.setOnClickListener { launchOrgDetails(org?.id) }
            } else {
                majorLocationText.text = EgOrg.getOrgNameSafe(clc.orgId)
                minorLocationText.visibility = View.GONE
            }
            copyCallNumberText.text = clc.callNumber
            copyLocationText.text = clc.copyLocation
            copyStatusesText.text = clc.countsByStatusLabel
        }

        private fun launchOrgDetails(orgId: Int?) {
            val context = itemView.context
            val intent = Intent(context, OrgDetailsActivity::class.java)
            intent.putExtra(Key.ORG_ID, orgId)
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.copy_information_item, viewGroup, false)
        return ViewHolder(v, groupCopiesBySystem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int = items.size
}
