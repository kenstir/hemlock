/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package net.kenstir.ui.view.search

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert
import net.kenstir.data.model.BibRecord
import net.kenstir.ui.App
import org.evergreen_ils.gateway.GatewayClient

/**
 * Provide views to RecyclerView with data from records.
 */
class RecordViewAdapter(private val records: List<BibRecord>) : RecyclerView.Adapter<RecordViewAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val recordImage: ImageView = v.findViewById(R.id.search_record_img)
        private val titleText: TextView = v.findViewById(R.id.search_record_title)
        private val authorText: TextView = v.findViewById(R.id.search_record_author)
        private val iconFormatText: TextView = v.findViewById(R.id.search_record_format)
        private val publisherText: TextView = v.findViewById(R.id.search_record_publishing)

        fun bindView(record: BibRecord) {
            Log.d(TAG, "id:${record.id} bindView")
            val context = recordImage.context

            // TODO: use a service method to get the image URL
            val url = GatewayClient.getUrl("/opac/extras/ac/jacket/small/r/" + record.id)

            recordImage.load(url)

            val scope = (context as? BaseActivity)?.lifecycleScope ?: return
            scope.async {
                try {
                    scope.async {
                        App.getServiceConfig().biblioService.loadRecordDetails(record, true)
                        loadMetadata(record)
                    }

                    scope.async {
                        App.getServiceConfig().biblioService.loadRecordAttributes(record)
                        loadFormat(record)
                    }

                } catch (ex: Exception) {
                    (context as? Activity)?.showAlert(ex)
                }
            }
        }

        private fun loadMetadata(record: BibRecord) {
            Log.d(TAG, "id:${record.id} title:${record.title}")
            titleText.text = record.title
            authorText.text = record.author
            publisherText.text = record.publishingInfo
        }

        private fun loadFormat(record: BibRecord) {
            Log.d(TAG, "id:${record.id} format:${record.iconFormatLabel}")
            iconFormatText.text = record.iconFormatLabel
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.search_result_item, viewGroup, false)
        return ViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val record = records[position]
        viewHolder.bindView(record)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return records.size
    }

    companion object {
        private val TAG = RecordViewAdapter::class.java.simpleName
    }
}
