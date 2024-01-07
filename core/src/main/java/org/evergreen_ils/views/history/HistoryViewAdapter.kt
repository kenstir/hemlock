/*
 * Copyright (c) 2024 Kenneth H. Cox
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

package org.evergreen_ils.views.history

import android.net.Network
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.NetworkImageView
import org.evergreen_ils.R
import org.evergreen_ils.data.HistoryRecord
import java.text.DateFormat

class HistoryViewAdapter(private val items: List<HistoryRecord>) : RecyclerView.Adapter<HistoryViewAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val recordImage: NetworkImageView = v.findViewById(R.id.search_record_img)
        private val title: TextView = v.findViewById(R.id.search_record_title)
        private val author: TextView = v.findViewById(R.id.search_record_author)
        private val checkoutDate: TextView = v.findViewById(R.id.item_checkout_date)
        private val returnDate: TextView = v.findViewById(R.id.item_return_date)

        fun bindView(historyRecord: HistoryRecord) {
            title.text = historyRecord.title
            author.text = historyRecord.author
            checkoutDate.text = historyRecord.checkoutDateString
            returnDate.text = historyRecord.returnedDateString
            // TODO: alter returnDate appearance if not returned?
//            val primaryStyle = if (historyRecord.isRead) R.style.HemlockText_ListPrimaryRead else R.style.HemlockText_ListPrimary
//            val secondaryStyle = if (historyRecord.isRead) R.style.HemlockText_ListSecondaryRead else R.style.HemlockText_ListSecondary
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                date.setTextAppearance(primaryStyle)
//            } else {
//                date.setTextAppearance(date.context, primaryStyle)
//            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.history_list_item, viewGroup, false)
        return ViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val message = items[position]
        viewHolder.bindView(message)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }
}
