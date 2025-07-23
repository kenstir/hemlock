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

package net.kenstir.ui.view.bookbags

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.ListItem
import net.kenstir.hemlock.R

class ListItemViewAdapter(
    private val items: List<ListItem>,
    private val onRemoveItem: (ListItem) -> Unit,
) : RecyclerView.Adapter<ListItemViewAdapter.ViewHolder>() {

    class ViewHolder(
        v: View,
        private val onRemoveItem: (ListItem) -> Unit,
    ) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.bookbagitem_title)
        val author: TextView = v.findViewById(R.id.bookbagitem_author)
        val pubdate: TextView = v.findViewById(R.id.bookbagitem_pubdate)
        val removeButton: Button = v.findViewById(R.id.bookbagitem_remove_button)

        fun bindView(item: ListItem) {
            val record = item.record
            title.text = record?.title
            author.text = record?.author
            pubdate.text = record?.pubdate
            removeButton.setOnClickListener { onRemoveItem(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.bookbagitem_list_item, parent, false)
        return ViewHolder(v, onRemoveItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int = items.size
}
