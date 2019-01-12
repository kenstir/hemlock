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

import java.util.ArrayList;

import android.support.v4.app.FragmentPagerAdapter;
import android.view.MenuItem;
import org.evergreen_ils.R;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.*;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;

public class SampleUnderlinesNoFade extends BasePagerActivity {
    private static final String TAG = SampleUnderlinesNoFade.class.getSimpleName();

    private ArrayList<RecordInfo> records;
    //private SearchCatalog search;
    private ArrayList<RecordInfo> searchRecords;
    private Context context;
    private ProgressDialog progressDialog;
    //private Runnable searchRunnableWithOffset;

    public static final int RETURN_DATA = 5;
    private Integer orgID = 1;
    private Integer numResults = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.simple_underlines);
        ActionBarUtils.initActionBarForActivity(this, getIntent().getStringExtra("title"));

        //search = SearchCatalog.getInstance();

        orgID = getIntent().getIntExtra("orgID", 1);
        records = (ArrayList<RecordInfo>) getIntent().getSerializableExtra("recordList");
        int record_position = getIntent().getIntExtra("recordPosition", 0);
        numResults = getIntent().getIntExtra("numResults", records.size());

        context = this;
        
        mAdapter = new SearchFragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(record_position);

        UnderlinePageIndicator indicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setFades(false);
        mIndicator = indicator;
        /*
        searchRunnableWithOffset = new Runnable() {

            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        progressDialog = ProgressDialog.show(context, null,getResources().getText(R.string.dialog_load_more_message));
                    }
                });
                
                searchRecords = search.getSearchResults(search.searchText, search.searchClass, search.searchFormat, records.size());

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (searchRecords.size() > 0) {
                            for (int j = 0; j < searchRecords.size(); j++)
                                records.add(searchRecords.get(j));
                        }
                        mAdapter.notifyDataSetChanged();
                        progressDialog.dismiss();
                    }
                });
            }
        };
        */
    }

    private void finishWithIntent() {
        Intent intent = new Intent();
        intent.putExtra("recordList", records);
        Log.d(TAG, "kcxxx about to setResult with "+records.size()+" items");
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
//            if (records.size() > 1 && position == records.size() - 1 && records.size() < numResults) {
//                Thread getSearchResults = new Thread(searchRunnableWithOffset);
//                getSearchResults.start();
//            }
            return BasicDetailsFragment.newInstance(records.get(position), position, numResults, orgID);
        }

        @Override
        public int getCount() {
            return records.size();
        }
    }
}
