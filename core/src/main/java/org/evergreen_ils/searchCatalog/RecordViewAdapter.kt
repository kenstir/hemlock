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
package org.evergreen_ils.searchCatalog

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.NetworkImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.net.Gateway.getUrl
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.net.Volley
import org.evergreen_ils.utils.ui.showAlert

/**
 * Provide views to RecyclerView with data from records.
 */
class RecordViewAdapter(private val records: List<MBRecord>) : RecyclerView.Adapter<RecordViewAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val recordImage: NetworkImageView = v.findViewById(R.id.search_record_img)
        private val titleText: TextView = v.findViewById(R.id.search_record_title)
        private val authorText: TextView = v.findViewById(R.id.search_record_author)
        private val iconFormatText: TextView = v.findViewById(R.id.search_record_format)
        private val publisherText: TextView = v.findViewById(R.id.search_record_publishing)

        fun bindView(record: MBRecord) {
            Log.d(TAG, "id:${record.id} bindView")
            val context = recordImage.context
            // todo is it better to load /jacket/medium/ here so it is cached for the details view?
            val url = getUrl("/opac/extras/ac/jacket/small/r/" + record.id)
            recordImage.setImageUrl(url, Volley.getInstance(context).imageLoader)
            //recordImage.setDefaultImageResId(R.drawable.missing_art);//for screenshots

            val coroutineScope = (context as? CoroutineScope) ?: return
            coroutineScope.async {
                try {
                    async {
                        GatewayLoader.loadRecordMetadataAsync(record)
                        loadMetadata(record)
                    }

                    async {
                        GatewayLoader.loadRecordAttributesAsync(record)
                        loadFormat(record)
                    }

                } catch (ex: Exception) {
                    (context as? Activity)?.showAlert(ex)
                }
            }
        }

        private fun loadMetadata(record: MBRecord) {
            Log.d(TAG, "id:${record.id} title:${record.title}")
            titleText.text = record.title
            authorText.text = record.author
            publisherText.text = record.publishingInfo
        }

        private fun loadFormat(record: MBRecord) {
            Log.d(TAG, "id:${record.id} format:${record.iconFormatLabel}")
            iconFormatText.text = record.iconFormatLabel
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view.
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
