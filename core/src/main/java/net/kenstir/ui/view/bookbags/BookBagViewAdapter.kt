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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.PatronList
import net.kenstir.hemlock.R

class BookBagViewAdapter(private val items: MutableList<PatronList>) : RecyclerView.Adapter<BookBagViewAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.bookbag_name)
        private val description: TextView = v.findViewById(R.id.bookbag_description)
        private val itemCount: TextView = v.findViewById(R.id.bookbag_items)

        fun bindView(patronList: PatronList) {
            name.text = patronList.name
            description.text = patronList.description
            itemCount.text = itemView.resources.getQuantityString(R.plurals.number_of_items,
                patronList.items.size, patronList.items.size)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.bookbag_list_item, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int = items.size
}
