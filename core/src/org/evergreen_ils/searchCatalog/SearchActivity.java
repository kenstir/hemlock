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

import java.io.Serializable;
import java.util.*;

import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.evergreen_ils.App;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.bookbags.BookBag;
import org.evergreen_ils.accountAccess.bookbags.BookBagUtils;
import org.evergreen_ils.accountAccess.holds.PlaceHoldActivity;
import org.evergreen_ils.barcodescan.CaptureActivity;
import org.evergreen_ils.billing.BillingHelper;
import org.evergreen_ils.globals.AppState;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.DonateActivity;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemSelectedListener;

public class SearchActivity extends ActionBarActivity {

    private static final String TAG = SearchActivity.class.getSimpleName();

    public static final String SEARCH_OPTIONS_VISIBLE = "search_options_visible";

    private EditText searchText;
    private SwitchCompat searchOptionsButton;
    private View searchOptionsLayout;
    private Button searchButton;
    private Spinner searchOrgSpinner;
    private Spinner searchClassSpinner;
    private Spinner searchFormatSpinner;
    private TextView searchResultsSummary;
    private SearchResultsFragment searchResultsFragment;

    private SearchCatalog search;
    private ArrayList<RecordInfo> recordList;
    private Context context;
    private ProgressDialog progressDialog;
    private ArrayList<RecordInfo> searchResults;
    private GlobalConfigs globalConfigs;
    private ArrayList<BookBag> bookBags;
    private Integer bookbag_selected = -1;
    private Runnable searchForResultsRunnable = null;

    private boolean loadingElements = false;
    private boolean searchOptionsVisible = true;
    private ContextMenuRecordInfo contextMenuRecordInfo;

    private String getSearchText() {
        return searchText.getText().toString();
    }

    private String getSearchClass() {
        return searchClassSpinner.getSelectedItem().toString().toLowerCase();
    }

    private String getSearchFormat() {
        return SearchFormat.getSearchFormatFromSpinnerLabel(searchFormatSpinner.getSelectedItem().toString());
    }

    private class ContextMenuRecordInfo implements ContextMenuInfo {
        public RecordInfo record;
        public int position;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }
        SearchFormat.init(this);
        AppState.init(this);

        setContentView(R.layout.search_layout3);
        ActionBarUtils.initActionBarForActivity(this);

        context = this;
        globalConfigs = GlobalConfigs.getInstance(this);
        search = SearchCatalog.getInstance();
        bookBags = AccountAccess.getInstance().getBookbags();
        recordList = new ArrayList<RecordInfo>();
        searchResults = new ArrayList<RecordInfo>();
        progressDialog = new ProgressDialog(context);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            searchResultsFragment = new SearchResultsFragment();
            Bundle args = new Bundle();
            args.putSerializable("recordList", recordList);
            searchResultsFragment.setArguments(args);
            transaction.replace(R.id.search_results_list, searchResultsFragment);
            transaction.commit();
        } else {
            searchResultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.search_results_list);
        }

        searchText = (EditText) findViewById(R.id.searchText);
        searchOptionsButton = (SwitchCompat) findViewById(R.id.search_options_button);
        searchOptionsLayout = findViewById(R.id.search_options_layout);
        searchButton = (Button) findViewById(R.id.search_button);
        searchClassSpinner = (Spinner) findViewById(R.id.search_qtype_spinner);
        searchFormatSpinner = (Spinner) findViewById(R.id.search_format_spinner);
        searchOrgSpinner = (Spinner) findViewById(R.id.search_org_spinner);
        searchResultsSummary = (TextView) findViewById(R.id.search_result_number);

        initSearchOptionsVisibility();
        initSearchText();
        initSearchOptionsButton();
        initSearchButton();
        initSearchFormatSpinner();
        initSearchOrgSpinner();
        initSearchRunnable();
    }

    private void initSearchButton() {
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread searchThread = new Thread(searchForResultsRunnable);
                searchThread.start();
            }
        });
    }

    private void initSearchOptionsVisibility() {
        boolean last_state = AppState.getBoolean(SEARCH_OPTIONS_VISIBLE, true);
        searchOptionsButton.setChecked(last_state);
        setSearchOptionsVisibility(last_state);
    }

    private void setSearchOptionsVisibility(boolean visible) {
        searchOptionsLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        searchOptionsVisible = visible;
        AppState.setBoolean(SEARCH_OPTIONS_VISIBLE, visible);
    }

    private void toggleSearchOptions() {
        setSearchOptionsVisibility(!searchOptionsVisible);
    }

    private void initSearchOptionsButton() {
        searchOptionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSearchOptions();
            }
        });
    }

    private void initSearchText() {
        // enter key now is labeled "Search" on virtual keyboard
        searchText.setImeActionLabel("Search", EditorInfo.IME_ACTION_SEARCH);
        searchText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // enter key on virtual keyboard starts the search
        searchText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && ((keyCode == KeyEvent.KEYCODE_ENTER) || keyCode == EditorInfo.IME_ACTION_SEARCH)) {
                    // Perform action on key press
                    Thread searchThread = new Thread(searchForResultsRunnable);
                    searchThread.start();
                    return true;
                }
                return false;
            }
        });
    }

    private void initSearchRunnable() {
        searchForResultsRunnable = new Runnable() {

            @Override
            public void run() {

                final String text = getSearchText();
                if (text.length() < 1)
                    return;
                int searchQueryType = searchClassSpinner.getSelectedItemPosition();
                Log.d(TAG, "type="+searchQueryType+" class="+getSearchClass()+" format="+getSearchFormat());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
                        searchResultsSummary.setVisibility(View.VISIBLE);
                        progressDialog = ProgressDialog.show(context,
                                getResources().getText(R.string.dialog_please_wait),
                                getResources().getText(R.string.dialog_fetching_data_message));
                    }
                });

                searchResults = search.getSearchResults(text, getSearchClass(), getSearchFormat(), 0);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordList.clear();
                        for (RecordInfo record : searchResults) {
                            recordList.add(record);
                        }

                        searchResultsSummary.setText(String.format(getString(R.string.n_of_m_results), recordList.size(), search.visible));
                        searchResultsFragment.notifyDatasetChanged();
                        initRecordClickListener();
                        progressDialog.dismiss();
                    }
                });

            }
        };
    }

    private void initSearchOrgSpinner() {
        int selectedOrgPos = 0;
        int homeLibrary = 0;
        if (AccountAccess.getInstance() != null) {
            homeLibrary = AccountAccess.getInstance().getHomeLibraryID();
        }
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < globalConfigs.organisations.size(); i++) {
            Organisation org = globalConfigs.organisations.get(i);
            list.add(org.indentedDisplayPrefix + org.name);
            if (org.id == homeLibrary) {
                selectedOrgPos = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.org_item_layout, list);
        searchOrgSpinner.setAdapter(adapter);
        searchOrgSpinner.setSelection(selectedOrgPos);
        search.selectOrganisation(globalConfigs.organisations.get(selectedOrgPos));
        searchOrgSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int ID, long arg3) {
                search.selectOrganisation(globalConfigs.organisations.get(ID));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    // unpack the json map to populate our spinner, and allow translation from search_format keyword <=> label
    private void initSearchFormatSpinner() {
        List<String> labels = SearchFormat.getSpinnerLabels();
        //ArrayAdapter<String> adapter = CompatSpinnerAdapter.CreateCompatSpinnerAdapter(this, labels);
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels);
        searchFormatSpinner.setAdapter(adapter);
    }

    private void initRecordClickListener() {
        registerForContextMenu(findViewById(R.id.search_results_list));
        searchResultsFragment.setOnRecordClickListener(new RecordInfo.OnRecordClickListener() {
            @Override
            public void onClick(RecordInfo record, int position) {
                Intent intent = new Intent(getBaseContext(), SampleUnderlinesNoFade.class);
                //todo add package prefix to names in putExtra
                intent.putExtra("recordInfo", record);
                intent.putExtra("recordList", recordList);
                intent.putExtra("recordPosition", position);
                intent.putExtra("numResults", search.visible);
                intent.putExtra("orgID", search.selectedOrganization.id);
                startActivityForResult(intent, 10);
            }
        });
        searchResultsFragment.setOnRecordLongClickListener(new RecordInfo.OnRecordLongClickListener() {
            @Override
            public void onLongClick(RecordInfo record, int position) {
                Log.d(TAG, "long click");
                // Don't know how to do this the Right Way so this will do for now.
                // Guess I could implement a custom View and override getContextMenuInfo().
                contextMenuRecordInfo = new ContextMenuRecordInfo();
                contextMenuRecordInfo.record = record;
                contextMenuRecordInfo.position = position;
                openContextMenu(findViewById(R.id.search_results_list));
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.search_results_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Options");
            menu.add(Menu.NONE, App.ITEM_SHOW_DETAILS, 0, getString(R.string.show_details_message));
            menu.add(Menu.NONE, App.ITEM_PLACE_HOLD, 1, getString(R.string.hold_place_title));
            menu.add(Menu.NONE, App.ITEM_ADD_TO_LIST, 2, getString(R.string.add_to_my_list_message));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //ContextMenuRecordInfo info = (ContextMenuRecordInfo)item.getMenuInfo();
        ContextMenuRecordInfo info = contextMenuRecordInfo;
        if (info == null)
            return false;

        switch (item.getItemId()) {
        case App.ITEM_SHOW_DETAILS:
            Intent intent = new Intent(getBaseContext(), SampleUnderlinesNoFade.class);
            intent.putExtra("recordInfo", info.record);
            intent.putExtra("orgID", search.selectedOrganization.id);
            intent.putExtra("recordList", recordList);
            intent.putExtra("recordPosition", info.position);
            intent.putExtra("numResults", search.visible);
            startActivity(intent);
            return true;
        case App.ITEM_PLACE_HOLD:
            Intent hold_intent = new Intent(getBaseContext(), PlaceHoldActivity.class);
            hold_intent.putExtra("recordInfo", info.record);
            startActivity(hold_intent);
            return true;
        case App.ITEM_ADD_TO_LIST:
            if (bookBags.size() > 0) {
                BookBagUtils.showAddToListDialog(this, bookBags, info.record);
            } else {
                Toast.makeText(context, getText(R.string.msg_no_lists), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        String url = getString(R.string.ou_feedback_url);
        if (TextUtils.isEmpty(url))
            menu.removeItem(R.id.action_feedback);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_advanced_search) {
            startActivityForResult(new Intent(getApplicationContext(), AdvancedSearchActivity.class), 2);
            return true;
        } else if (id == R.id.action_feedback) {
            String url = getString(R.string.ou_feedback_url);
            if (!TextUtils.isEmpty(url)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        } else if (id == R.id.action_donate) {
            startActivityForResult(new Intent(this, DonateActivity.class), BillingHelper.REQUEST_PURCHASE);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void replaceRecordList(ArrayList<RecordInfo> newRecords) {
        if (newRecords != null) {
            recordList.clear();
            for (RecordInfo record : newRecords) {
                recordList.add(record);
            }
            searchResultsFragment.notifyDatasetChanged();
            searchResultsSummary.setText(String.format(getString(R.string.n_of_m_results), recordList.size(), search.visible));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // todo we should not switch on resultCode here, we should switch on requestCode
        switch (resultCode) {
        case SampleUnderlinesNoFade.RETURN_DATA : {
            Serializable extra = null;
            if (data != null) extra = data.getSerializableExtra("recordList");
            if (extra != null) {
                replaceRecordList((ArrayList)extra);
            }
        }
        break;
        
        case AdvancedSearchActivity.RESULT_ADVANCED_SEARCH: {
            Log.d(TAG, "result text:" + data.getStringExtra("advancedSearchText"));
            searchText.setText(data.getStringExtra("advancedSearchText"));
            Thread searchThread = new Thread(searchForResultsRunnable);
            searchThread.start();
        }
            break;

        case CaptureActivity.BARCODE_SEARCH: {
            searchText.setText("identifier|isbn: "
                    + data.getStringExtra("barcodeValue"));
            Thread searchThread = new Thread(searchForResultsRunnable);
            searchThread.start();
        }
        }
    }
}
