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

package net.kenstir.ui.view.search;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import android.view.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.kenstir.hemlock.R;

import net.kenstir.ui.App;
import net.kenstir.data.model.BibRecord;
import net.kenstir.ui.util.ItemClickSupport;

import java.util.List;

public class SearchResultsFragment extends Fragment {

    private static final String TAG = SearchResultsFragment.class.getSimpleName();
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2;

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;
    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected List<BibRecord> mDataset;
    protected RecordViewAdapter mAdapter;
    protected OnRecordClickListener mOnRecordClickListener;
    protected OnRecordLongClickListener mOnRecordLongClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: fix crash here
        // https://console.firebase.google.com/u/0/project/pines-e1f4d/crashlytics/app/android:net.kenstir.apps.pines/issues/50b75e6b15fd31e91a8f762c05539a9e?time=last-seven-days&types=crash&sessionEventKey=68842270027700010F08DBD37A17FF02_2109627947480090230
        mDataset = App.getServiceConfig().getSearchService().getLastSearchResults().getRecords();
        mAdapter = new RecordViewAdapter(mDataset);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);
        rootView.setTag(TAG);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);

        mLayoutManager = new LinearLayoutManager(getActivity());

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(container.getContext(), DividerItemDecoration.VERTICAL_LIST));

        ItemClickSupport cs = ItemClickSupport.addTo(mRecyclerView);
        cs.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                if (mOnRecordClickListener != null) {
                    BibRecord record = mDataset.get(position);
                    mOnRecordClickListener.onClick(record, position);
                }
            }
        });
        cs.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                if (mOnRecordLongClickListener != null) {
                    BibRecord record = mDataset.get(position);
                    mOnRecordLongClickListener.onLongClick(record, position);
                    return true;
                }
                return false;
            }
        });

        return rootView;
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        switch (layoutManagerType) {
            case GRID_LAYOUT_MANAGER:
                mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
                mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void notifyDatasetChanged() {
        mRecyclerView.scrollToPosition(0);
        mAdapter.notifyDataSetChanged();
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        mOnRecordClickListener = listener;
    }

    public void setOnRecordLongClickListener(OnRecordLongClickListener listener) {
        mOnRecordLongClickListener = listener;
    }

    public interface OnRecordClickListener {
        void onClick(BibRecord record, int position);
    }

    public interface OnRecordLongClickListener {
        void onLongClick(BibRecord record, int position);
    }
}
