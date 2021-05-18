/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 *
 */
package org.evergreen_ils.searchCatalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import org.evergreen_ils.R;
import org.evergreen_ils.android.App;
import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.system.EgSearch;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.DetailsFragment;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class RecordDetailsActivity extends BaseActivity {
    private static final String TAG = RecordDetailsActivity.class.getSimpleName();

    public ViewPager mPager;

    private ArrayList<RecordInfo> records = new ArrayList<>();

    public static final int RETURN_DATA = 5;
    private Integer orgID = 1;
    private Integer numResults = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!App.isStarted()) {
            App.restartApp(this);
            return;
        }

        setContentView(R.layout.record_details);
        ActionBarUtils.initActionBarForActivity(this, getIntent().getStringExtra("title"));

        // Copy either serialized recordList or search results into our own ArrayList.
        // This is an attempt to fix an IllegalStateException crash (see commit for details).
        ArrayList<RecordInfo> recordList = (ArrayList<RecordInfo>) getIntent().getSerializableExtra("recordList");
        if (recordList == null) recordList = EgSearch.INSTANCE.getResults();
        records.clear();
        records.addAll(recordList);

        // Calculate numResults after records are loaded
        orgID = getIntent().getIntExtra("orgID", EgOrg.consortiumID);
        int recordPosition = getIntent().getIntExtra("recordPosition", 0);
        numResults = getIntent().getIntExtra("numResults", records.size());

        mPager = findViewById(R.id.pager);
        mPager.setAdapter(new SearchFragmentAdapter(getSupportFragmentManager()));
        mPager.setCurrentItem(recordPosition);
    }

    private void finishWithIntent() {
        Intent intent = new Intent();
        setResult(RETURN_DATA, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishWithIntent();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //onBackPressed();
            finishWithIntent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class SearchFragmentAdapter extends FragmentStatePagerAdapter {
        public SearchFragmentAdapter(FragmentManager fm) {
            //super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT); //compatible
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT); //recommended
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return DetailsFragment.create(records.get(position), orgID, position, numResults);
        }

        @Override
        public int getCount() {
            return records.size();
        }
    }
}
