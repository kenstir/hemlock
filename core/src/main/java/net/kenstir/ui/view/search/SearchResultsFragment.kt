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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.BibRecord
import net.kenstir.hemlock.R
import net.kenstir.ui.App
import net.kenstir.ui.util.ItemClickSupport

class SearchResultsFragment : Fragment() {
    enum class LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    protected var mCurrentLayoutManagerType: LayoutManagerType? = null
    protected var mRecyclerView: RecyclerView? = null
    protected var mLayoutManager: RecyclerView.LayoutManager? = null
    protected var mDataset: List<BibRecord> = emptyList()
    protected var mAdapter: RecordViewAdapter? = null
    protected var mOnRecordClickListener: OnRecordClickListener? = null
    protected var mOnRecordLongClickListener: OnRecordLongClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDataset = App.svc.searchService.getLastSearchResults().records
        mAdapter = RecordViewAdapter(mDataset)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.recycler_view_frag, container, false)
        rootView.tag = TAG

        mRecyclerView = rootView.findViewById(R.id.recycler_view)

        mLayoutManager = LinearLayoutManager(activity)

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER) as LayoutManagerType?
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType!!)

        mRecyclerView?.setAdapter(mAdapter)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        val cs = ItemClickSupport.addTo(mRecyclerView)
        cs.setOnItemClickListener { _, position, _ ->
            if (position < 0 || position >= mDataset.size) return@setOnItemClickListener
            mOnRecordClickListener?.onClick(mDataset[position], position)
        }
        cs.setOnItemLongClickListener { _, position, _ ->
            if (position < 0 || position >= mDataset.size) return@setOnItemLongClickListener false
            mOnRecordLongClickListener?.onLongClick(mDataset[position], position)
            return@setOnItemLongClickListener true
        }

        return rootView
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    fun setRecyclerViewLayoutManager(layoutManagerType: LayoutManagerType) {
        var scrollPosition = 0

        // If a layout manager has already been set, get current scroll position.
        (mRecyclerView?.layoutManager as? LinearLayoutManager)?.let { lm ->
            scrollPosition = lm.findFirstCompletelyVisibleItemPosition()
        }

        when (layoutManagerType) {
            LayoutManagerType.GRID_LAYOUT_MANAGER -> {
                mLayoutManager = GridLayoutManager(activity, SPAN_COUNT)
                mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER
            }
            else -> {
                mLayoutManager = LinearLayoutManager(activity)
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER
            }
        }

        mRecyclerView?.setLayoutManager(mLayoutManager)
        mRecyclerView?.scrollToPosition(scrollPosition)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType)
        super.onSaveInstanceState(savedInstanceState)
        //logBundleSize(savedInstanceState);
    }

    fun notifyDatasetChanged() {
        mRecyclerView?.scrollToPosition(0)
        mAdapter?.notifyDataSetChanged()
    }

    fun setOnRecordClickListener(listener: OnRecordClickListener?) {
        mOnRecordClickListener = listener
    }

    fun setOnRecordLongClickListener(listener: OnRecordLongClickListener?) {
        mOnRecordLongClickListener = listener
    }

    fun interface OnRecordClickListener {
        fun onClick(record: BibRecord?, position: Int)
    }

    fun interface OnRecordLongClickListener {
        fun onLongClick(record: BibRecord?, position: Int)
    }

    companion object {
        private const val TAG = "SearchResultsFragment"
        private const val KEY_LAYOUT_MANAGER = "layoutManager"
        private const val SPAN_COUNT = 2
    }
}
