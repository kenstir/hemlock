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

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.utils.ui.*;
import org.evergreen_ils.views.AccountScreenDashboard;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SampleUnderlinesNoFade extends ActionBarActivity {

    private ArrayList<RecordInfo> records;

    private Button myAccountButton;

    private Button homeButton;

    private TextView headerTitle;
    
    private SearchCatalog search;
    
    private ArrayList<RecordInfo> searchRecords;
    
    private Context context;

    private ProgressDialog progressDialog;
    
    private Runnable searchRunnableWithOffset;

    public TestFragmentAdapter mAdapter;
    public ViewPager mPager;
    public PageIndicator mIndicator;

    public static final int RETURN_DATA = 5;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.simple_underlines);

        // set up action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(AccountAccess.userName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        search = SearchCatalog.getInstance((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));

        records = (ArrayList<RecordInfo>) getIntent().getSerializableExtra("recordList");

        if (records.get(records.size() - 1).dummy == true)
            records.remove(records.size() - 1);

        context = this;
        
        int record_position = getIntent().getIntExtra("recordPosition", 0);
        mAdapter = new SearchFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mPager.setCurrentItem(record_position);

        UnderlinePageIndicator indicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setFades(false);
        mIndicator = indicator;
        searchRunnableWithOffset = new Runnable() {

            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        progressDialog = ProgressDialog.show(context, null,getResources().getText(R.string.dialog_load_more_message));
                    }
                });
                
                searchRecords = search.getSearchResults(search.searchText, records.size());

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // don't clear record list
                        // recordList.clear();
                        if (searchRecords.size() > 0) {

                            for (int j = 0; j < searchRecords
                                    .size(); j++)
                                records.add(searchRecords.get(j));

                            // add extra record to display more
                            // option button

                        }
                        progressDialog.dismiss();
                    }
                });

            }
        };

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.putExtra("recordList", records);
            setResult(RETURN_DATA, intent);
            finish();
            
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    
    class SearchFragmentAdapter extends TestFragmentAdapter {
        public SearchFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // position +1 for 1 - size values
            
            if(position == records.size() - 1 && records.size() < search.visible){
                    Thread getSearchResults = new Thread(searchRunnableWithOffset);
                    getSearchResults.start();
            }
            return BasicDetailsFragment.newInstance(records.get(position),
                    position + 1, search.visible);
            
        }

        @Override
        public int getCount() {
            return records.size();
            // return TabsView.CONTENT.length;
        }

    }
}