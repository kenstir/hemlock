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

package org.evergreen_ils.searchCatalog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.view.*;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.AdapterView;
import org.evergreen_ils.App;
import org.evergreen_ils.R;
import org.evergreen_ils.globals.Log;

import java.util.ArrayList;

public class RecyclerViewFragment extends Fragment {

    private static final String TAG = RecyclerViewFragment.class.getSimpleName();
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2;

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;
    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ArrayList<RecordInfo> mDataset;
    protected RecordViewAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = this.getArguments();
        mDataset = (ArrayList<RecordInfo>) args.getSerializable("recordList");
        mAdapter = new RecordViewAdapter(mDataset);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);
        rootView.setTag(TAG);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        mLayoutManager = new LinearLayoutManager(getActivity());

        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(container.getContext(), DividerItemDecoration.VERTICAL_LIST));
        registerForContextMenu(mRecyclerView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(TAG, "context menu");
        int pos = ((RecyclerView.LayoutParams) v.getLayoutParams()).getViewLayoutPosition();
        Log.d(TAG, "Context: pos=" + pos);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        //menu.setHeaderTitle("Options");
        menu.add(Menu.NONE, App.ITEM_SHOW_DETAILS, 0, getString(R.string.show_details_message));
        menu.add(Menu.NONE, App.ITEM_PLACE_HOLD, 1, getString(R.string.hold_place_title));
        menu.add(Menu.NONE, App.ITEM_ADD_TO_LIST, 2, getString(R.string.add_to_my_list_message));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuArrayItem = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int pos = menuArrayItem.position;
        Log.d(TAG, "here, pos=" + pos + " item_id=" + item.getItemId());
        long id = mRecyclerView.getAdapter().getItemId(pos);;
        Log.d(TAG, "here, id="+id);
        /* todo needs work!

        switch (item.getItemId()) {
        case ITEM_SHOW_DETAILS:
            Intent intent = new Intent(getBaseContext(), SampleUnderlinesNoFade.class);
            intent.putExtra("recordInfo", info);
            intent.putExtra("orgID", search.selectedOrganization.id);
            intent.putExtra("recordList", recordList);
            intent.putExtra("recordPosition", menuArrayItem.position);
            intent.putExtra("numResults", search.visible);
            startActivity(intent);
            break;
        case ITEM_PLACE_HOLD:
            Intent hold_intent = new Intent(getBaseContext(), PlaceHoldActivity.class);
            hold_intent.putExtra("recordInfo", info);
            startActivity(hold_intent);
            break;
        case ITEM_ADD_TO_LIST:
            if (bookBags.size() > 0) {
                BookBagUtils.showAddToListDialog(this, bookBags, info);
            } else {
                Toast.makeText(context, getText(R.string.msg_no_lists), Toast.LENGTH_SHORT).show();
            }
            break;
        }
        */

        return super.onContextItemSelected(item);
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
        mAdapter.notifyDataSetChanged();
    }

    public void setOnRecordClickListener(RecordInfo.OnRecordClickListener listener) {
        mAdapter.setOnRecordClickListener(listener);
    }
}
