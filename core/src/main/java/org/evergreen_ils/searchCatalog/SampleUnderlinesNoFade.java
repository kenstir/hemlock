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
import org.evergreen_ils.system.EgSearch;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.BasePagerActivity;
import org.evergreen_ils.utils.ui.DetailsFragment;
import org.evergreen_ils.utils.ui.UnderlinePageIndicator;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class SampleUnderlinesNoFade extends BasePagerActivity {
    private static final String TAG = SampleUnderlinesNoFade.class.getSimpleName();

    private ArrayList<RecordInfo> records;

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

        setContentView(R.layout.simple_underlines);
        ActionBarUtils.initActionBarForActivity(this, getIntent().getStringExtra("title"));

        orgID = getIntent().getIntExtra("orgID", 1);
        records = (ArrayList<RecordInfo>) getIntent().getSerializableExtra("recordList");
        if (records == null) records = EgSearch.INSTANCE.getResults();
        int record_position = getIntent().getIntExtra("recordPosition", 0);
        numResults = getIntent().getIntExtra("numResults", records.size());

        mAdapter = new SearchFragmentAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(record_position);

        UnderlinePageIndicator indicator = findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setFades(false);
        mIndicator = indicator;
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

    class SearchFragmentAdapter extends FragmentPagerAdapter {
        public SearchFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DetailsFragment.newInstance(records.get(position), position, numResults, orgID);
        }

        @Override
        public int getCount() {
            return records.size();
        }
    }
}
