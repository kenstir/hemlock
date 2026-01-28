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
import net.kenstir.data.model.BibRecord
import net.kenstir.data.service.ImageSize
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert

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

            val url = App.svc.biblio.imageUrl(record, ImageSize.SMALL)
            recordImage.load(url)

            val scope = (context as? BaseActivity)?.lifecycleScope ?: return
            scope.async {
                try {
                    scope.async {
                        App.svc.biblio.loadRecordDetails(record, true)
                        loadMetadata(record)
                    }

                    scope.async {
                        App.svc.biblio.loadRecordAttributes(record)
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.search_result_item, parent, false)
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
        private const val TAG = "RecordViewAdapter"
    }
}
