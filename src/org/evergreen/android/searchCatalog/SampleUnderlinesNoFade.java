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
package org.evergreen.android.searchCatalog;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.utils.ui.BaseSampleActivity;
import org.evergreen.android.utils.ui.BasicDetailsFragment;
import org.evergreen.android.utils.ui.TestFragmentAdapter;
import org.evergreen.android.utils.ui.UnderlinePageIndicator;
import org.evergreen.android.views.AccountScreenDashboard;

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

public class SampleUnderlinesNoFade extends BaseSampleActivity {

    private ArrayList<RecordInfo> records;

    private Button myAccountButton;

    private Button homeButton;

    private TextView headerTitle;
    
    private SearchCatalog search;
    
    private ArrayList<RecordInfo> searchRecords;
    
    private Context context;

    private ProgressDialog progressDialog;
    
    private Runnable searchRunnableWithOffset;
    
    public static final int RETURN_DATA = 5;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_underlines);

        search = SearchCatalog.getInstance((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));
        
        records = (ArrayList<RecordInfo>) getIntent().getSerializableExtra(
                "recordList");

        if (records.get(records.size() - 1).dummy == true)
            records.remove(records.size() - 1);

        context = this;
        
        // header portion actions
        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.search_details_title);

        myAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        AccountScreenDashboard.class);
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        SearchCatalogListView.class);
                startActivity(intent);
            }
        });
        // end header portion actions

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
                        progressDialog = ProgressDialog.show(context,null ,getResources().getText(R.string.dialog_load_more_message));
                    }
                });
                
                try {
                    searchRecords = search.getSearchResults(search.searchText,
                            records.size());
                } catch (NoNetworkAccessException e) {
                    runOnUiThread(Utils
                            .showNetworkNotAvailableDialog(context));
                } catch (NoAccessToServer e) {
                    runOnUiThread(Utils
                            .showServerNotAvailableDialog(context));
                }

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