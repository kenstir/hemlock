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

package net.kenstir.ui.view.history

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.HistoryRecord
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert
import org.evergreen_ils.gateway.GatewayClient

class HistoryViewAdapter(private val items: List<HistoryRecord>) : RecyclerView.Adapter<HistoryViewAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val recordImage: ImageView = v.findViewById(R.id.search_record_img)
        private val title: TextView = v.findViewById(R.id.search_record_title)
        private val author: TextView = v.findViewById(R.id.search_record_author)
        private val checkoutDate: TextView = v.findViewById(R.id.item_checkout_date)
        private val returnDate: TextView = v.findViewById(R.id.item_return_date)

        fun bindView(historyRecord: HistoryRecord) {
            Log.d(TAG, "id:${historyRecord.id} bindView")
            val context = title.context

            title.text = null
            author.text = null
            checkoutDate.text = String.format(context.getString(R.string.label_checkout_date), historyRecord.checkoutDateLabel)
            returnDate.text = String.format(context.getString(R.string.label_returned_date), historyRecord.returnedDateLabel)
            // TODO: clear recordImage?

            // TODO: alter returnDate appearance if not returned?
//            val primaryStyle = if (historyRecord.isRead) R.style.HemlockText_ListPrimaryRead else R.style.HemlockText_ListPrimary
//            val secondaryStyle = if (historyRecord.isRead) R.style.HemlockText_ListSecondaryRead else R.style.HemlockText_ListSecondary
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                date.setTextAppearance(primaryStyle)
//            } else {
//                date.setTextAppearance(date.context, primaryStyle)
//            }

            val scope = (context as? BaseActivity)?.lifecycleScope ?: return
            scope.async {
                try {
                    scope.async {
                        fetchCopyDetails(historyRecord)
                        loadMetadata(context, historyRecord)
                    }
                } catch (ex: Exception) {
                    (context as? Activity)?.showAlert(ex)
                }
            }
        }

        private suspend fun fetchCopyDetails(historyRecord: HistoryRecord): Result<Unit> {
            val targetCopy = historyRecord.targetCopy ?: return Result.Success(Unit)
            return net.kenstir.ui.App.getServiceConfig().circService.loadHistoryDetails(historyRecord)
        }

        private fun loadMetadata(context: Context, historyRecord: HistoryRecord) {
            Log.d(TAG, "id:${historyRecord.id} title:${historyRecord.title}")
            title.text = historyRecord.title
            author.text = historyRecord.author

            historyRecord.record?.id.let { id ->
                val url = GatewayClient.getUrl("/opac/extras/ac/jacket/small/r/" + id)
                recordImage.load(url)
            }
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

    companion object {
        private val TAG = HistoryViewAdapter::class.java.simpleName
    }
}
