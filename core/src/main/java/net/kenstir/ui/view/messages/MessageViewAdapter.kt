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

package net.kenstir.ui.view.messages

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.hemlock.R
import net.kenstir.data.model.PatronMessage
import java.text.DateFormat

class MessageViewAdapter(private val items: List<PatronMessage>) : RecyclerView.Adapter<MessageViewAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private val title: TextView = v.findViewById(R.id.message_title)
        private val date: TextView = v.findViewById(R.id.message_date)
        private val body: TextView = v.findViewById(R.id.message_body)

        fun bindView(message: PatronMessage) {
            title.text = message.title
            date.text = if (message.createDate != null) DateFormat.getDateInstance().format(message.createDate) else ""
            body.text = message.message.trim()
            val primaryStyle = if (message.isRead) R.style.HemlockText_ListPrimary_Faded else R.style.HemlockText_ListPrimary
            val secondaryStyle = if (message.isRead) R.style.HemlockText_ListSecondary_Faded else R.style.HemlockText_ListSecondary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title.setTextAppearance(primaryStyle)
                date.setTextAppearance(primaryStyle)
                body.setTextAppearance(secondaryStyle)
            } else {
                title.setTextAppearance(title.context, primaryStyle)
                date.setTextAppearance(date.context, primaryStyle)
                body.setTextAppearance(body.context, secondaryStyle)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.messages_list_item, parent, false)
        return ViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bindView(items[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }
}
