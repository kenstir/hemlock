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

package net.kenstir.ui.view.holds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.HoldRecord
import net.kenstir.hemlock.R

class HoldsViewAdapter(
    private val items: List<HoldRecord>,
    private val onEditClick: (HoldRecord) -> Unit,
) : RecyclerView.Adapter<HoldsViewAdapter.ViewHolder>() {

    class ViewHolder(
        view: View,
        private val onEditClick: (HoldRecord) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.hold_title)
        val authorText: TextView = view.findViewById(R.id.hold_author)
        val formatText: TextView = view.findViewById(R.id.hold_format)
        val statusText: TextView = view.findViewById(R.id.hold_status)
        val editButton: Button = view.findViewById(R.id.edit_button)

        fun bindView(record: HoldRecord) {
            titleText.text = record.title
            authorText.text = record.author
            formatText.text = record.formatLabel
            statusText.text = record.getHoldStatus(itemView.resources)
            editButton.setOnClickListener { onEditClick(record) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.holds_list_item, parent, false)
        return ViewHolder(view, onEditClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int = items.size
}
