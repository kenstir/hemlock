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

import java.util.*;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.*;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.accountAccess.bookbags.BookBag;
import org.evergreen_ils.accountAccess.holds.PlaceHoldActivity;
import org.evergreen_ils.barcodescan.CaptureActivity;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SearchCatalogListView extends ActionBarActivity {

    private static final String TAG = SearchCatalogListView.class.getSimpleName();

    private static final int PLACE_HOLD = 0;
    private static final int DETAILS = 1;
    private static final int BOOK_BAG = 2;
    public static final String SEARCH_OPTIONS_VISIBLE = "search_options_visible";

    private EditText searchText;
    private ImageButton searchOptionsButton;
    private View searchOptionsLayout;
    private ImageButton searchButton;
    private Spinner searchOrgSpinner;
    private Spinner searchClassSpinner;
    private Spinner searchFormatSpinner;
    private TextView searchResultsNumber;
    private ListView lv;

    private SearchCatalog search;
    private ArrayList<RecordInfo> recordList;
    private SearchArrayAdapter adapter;
    private Context context;
    private ProgressDialog progressDialog;
    private ArrayList<RecordInfo> searchResults;
    private GlobalConfigs globalConfigs;
    private ArrayList<BookBag> bookBags;
    private Integer bookbag_selected = -1;
    private Runnable searchForResultsRunnable = null;

    private boolean loadingElements = false;
    private boolean searchOptionsVisible = false;

    private String getSearchText() {
        return searchText.getText().toString();
    }

    private String getSearchClass() {
        return searchClassSpinner.getSelectedItem().toString().toLowerCase();
    }

    private String getSearchFormat() {
        return SearchFormat.getSearchFormatFromSpinnerLabel(searchFormatSpinner.getSelectedItem().toString());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }
        SearchFormat.init(this);
        AppPrefs.init(this);

        setContentView(R.layout.search_layout3);
        ActionBarUtils.initActionBarForActivity(this);

        context = this;
        globalConfigs = GlobalConfigs.getInstance(this);
        search = SearchCatalog.getInstance();
        bookBags = AccountAccess.getInstance().getBookbags();
        recordList = new ArrayList<RecordInfo>();
        searchResults = new ArrayList<RecordInfo>();
        progressDialog = new ProgressDialog(context);

        searchText = (EditText) findViewById(R.id.searchText);
        searchOptionsButton = (ImageButton) findViewById(R.id.search_options_button);
        searchOptionsLayout = findViewById(R.id.search_options_layout);
        searchButton = (ImageButton) findViewById(R.id.search_button);
        searchClassSpinner = (Spinner) findViewById(R.id.search_qtype_spinner);
        searchFormatSpinner = (Spinner) findViewById(R.id.search_format_spinner);
        searchOrgSpinner = (Spinner) findViewById(R.id.search_org_spinner);
        searchResultsNumber = (TextView) findViewById(R.id.search_result_number);
        lv = (ListView) this.findViewById(R.id.search_results_list);

        initSearchOptionsVisibility();
        initSearchText();
        initSearchOptionsButton();
        initSearchButton();
        initSearchFormatSpinner();
        initSearchOrgSpinner();
        initSearchListView();
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
        boolean last_state = AppPrefs.getBoolean(SEARCH_OPTIONS_VISIBLE, true);
        setSearchOptionsVisibility(last_state);
    }

    private void setSearchOptionsVisibility(boolean visible) {
        if (visible) {
            searchOptionsLayout.setVisibility(View.VISIBLE);
            searchOptionsButton.setImageResource(R.drawable.expander_ic_maximized);
        } else {
            searchOptionsLayout.setVisibility(View.GONE);
            searchOptionsButton.setImageResource(R.drawable.expander_ic_minimized);
        }
        searchOptionsVisible = visible;
        AppPrefs.setBoolean(SEARCH_OPTIONS_VISIBLE, visible);
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

    private void initSearchListView() {
        adapter = new SearchArrayAdapter(getApplicationContext(),
                R.layout.search_result_item, recordList);
        lv.setAdapter(adapter);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                RecordInfo info = (RecordInfo) lv.getItemAtPosition(position);
                Intent intent = new Intent(getBaseContext(), SampleUnderlinesNoFade.class);
                //todo add package prefix to names in putExtra
                intent.putExtra("recordInfo", info);
                intent.putExtra("orgID", search.selectedOrganization.id);
                intent.putExtra("depth", search.selectedOrganization.level);
                intent.putExtra("recordList", recordList);
                intent.putExtra("recordPosition", position);
                startActivityForResult(intent, 10);
            }
        });

        lv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

                if (!loadingElements) {
                    if (totalItemCount > 0
                            && (((totalItemCount - visibleItemCount) <= (firstVisibleItem)) && adapter
                                    .getCount() < search.visible)) {
                        loadingElements = true;
                        Log.d(TAG, "Load more data");
                        progressDialog = new ProgressDialog(context);

                        progressDialog.setMessage(getResources().getText(
                                R.string.dialog_load_more_message));
                        progressDialog.show();

                        Thread searchThreadwithOffset = new Thread(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        String text = getSearchText();
                                        searchResults.clear();
                                        searchResults = search.getSearchResults(text, getSearchClass(),
                                                getSearchFormat(),
                                                adapter.getCount());

                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {

                                                // don't clear record list
                                                // recordList.clear();
                                                Log.d(TAG, "Returned "
                                                        + searchResults.size()
                                                        + " elements from search");
                                                for (int j = 0; j < searchResults.size(); j++) {
                                                    recordList.add(searchResults.get(j));
                                                }

                                                searchResultsNumber.setText(adapter
                                                        .getCount()
                                                        + " out of "
                                                        + search.visible);

                                                adapter.notifyDataSetChanged();
                                                progressDialog.dismiss();
                                                loadingElements = false;
                                            }
                                        });

                                    }
                                });

                        searchThreadwithOffset.start();
                    }
                }
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
                        searchResultsNumber.setVisibility(View.VISIBLE);
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
                        if (searchResults.size() > 0) {
                            for (int j = 0; j < searchResults.size(); j++)
                                recordList.add(searchResults.get(j));
                        }

                        searchResultsNumber.setText(+recordList.size() + " out of " + search.visible);
                        adapter.notifyDataSetChanged();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {

        Log.d(TAG, "context menu");
        if (v.getId() == R.id.search_results_list) {

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Options");

            menu.add(Menu.NONE, DETAILS, 0, "Details");
            menu.add(Menu.NONE, PLACE_HOLD, 1, "Place Hold");
            menu.add(Menu.NONE, BOOK_BAG, 2, "Add to bookbag");

        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuArrayItem = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        int menuItemIndex = item.getItemId();

        final RecordInfo info = (RecordInfo) lv
                .getItemAtPosition(menuArrayItem.position);
        // start activity with book details

        switch (item.getItemId()) {

        case DETAILS: {

            Intent intent = new Intent(getBaseContext(),
                    SampleUnderlinesNoFade.class);
            // serialize object and pass it to next activity
            intent.putExtra("recordInfo", info);
            intent.putExtra("orgID", search.selectedOrganization.id);
            intent.putExtra("depth", search.selectedOrganization.level);

            intent.putExtra("recordList", recordList);
            // TODO put total number
            intent.putExtra("recordPosition", menuArrayItem.position);
            startActivity(intent);
        }
            break;
        case PLACE_HOLD: {

            Intent intent = new Intent(getBaseContext(), PlaceHoldActivity.class);

            intent.putExtra("recordInfo", info);

            startActivity(intent);
        }
            break;
        case BOOK_BAG: {

            if (bookBags.size() > 0) {
                String array_spinner[] = new String[bookBags.size()];

                for (int i = 0; i < array_spinner.length; i++)
                    array_spinner[i] = bookBags.get(i).name;

                AlertDialog.Builder builder;

                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.bookbag_spinner, null);

                Spinner s = (Spinner) layout.findViewById(R.id.bookbag_spinner);
                Button add = (Button) layout.findViewById(R.id.add_to_bookbag_button);
                ArrayAdapter adapter = new ArrayAdapter(context,
                        android.R.layout.simple_spinner_item, array_spinner);

                s.setAdapter(adapter);
                builder = new AlertDialog.Builder(context);
                builder.setView(layout);
                final AlertDialog alertDialog = builder.create();

                add.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Thread addtoBookbag = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                AccountAccess ac = AccountAccess.getInstance();
                                try {
                                    ac.addRecordToBookBag(info.doc_id,
                                            bookBags.get(bookbag_selected).id);
                                } catch (SessionNotFoundException e) {
                                    Log.d(TAG, "session not found", e);
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        alertDialog.dismiss();
                                    }
                                });

                            }
                        });
                        progressDialog = ProgressDialog.show(context,
                                getResources().getText(R.string.dialog_please_wait),
                                "Adding to bookbag");
                        addtoBookbag.start();

                    }
                });
                alertDialog.show();

                s.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        bookbag_selected = position;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }

                });
            } else
                Toast.makeText(context, "No bookbags", Toast.LENGTH_SHORT).show();
        }
            break;
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
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.ou_feedback_url)));
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode) {

        case SampleUnderlinesNoFade.RETURN_DATA : {
            ArrayList<RecordInfo> records = (ArrayList)data.getSerializableExtra("recordList");
            recordList.clear();
            for(int i=0;i<records.size();i++){
                recordList.add(records.get(i));
            }
            adapter.notifyDataSetChanged();
            searchResultsNumber.setText(adapter.getCount()
                    + " out of " + search.visible);
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

    class SearchArrayAdapter extends ArrayAdapter<RecordInfo> {

        private Context context;
        private List<RecordInfo> records = new ArrayList<RecordInfo>();

        public SearchArrayAdapter(Context context, int textViewResourceId, List<RecordInfo> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public RecordInfo getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            // if it is the right type of view
            if (row == null || row.findViewById(R.id.search_record_title) == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.search_result_item, parent, false);
            }

            // Get item
            RecordInfo record = getItem(position);

            // Start async image load
            NetworkImageView recordImage = (NetworkImageView) row.findViewById(R.id.search_record_img);
            final String imageHref = GlobalConfigs.getUrl("/opac/extras/ac/jacket/small/r/" + record.doc_id);
            ImageLoader imageLoader = VolleyWrangler.getInstance(context).getImageLoader();
            recordImage.setImageUrl(imageHref, imageLoader);

            // Start async record load
            fetchRecordInfo(row, record);

            return row;
        }

        private void updateBasicMetadataViews(View row, RecordInfo record) {
            ((TextView) row.findViewById(R.id.search_record_title)).setText(record.title);
            ((TextView) row.findViewById(R.id.search_record_author)).setText(record.author);
            ((TextView) row.findViewById(R.id.search_record_publishing)).setText(record.getPublishingInfo());
        }

        private void updateSearchFormatView(View row, RecordInfo record) {
            ((TextView) row.findViewById(R.id.search_record_format)).setText(
                    SearchFormat.getItemLabelFromSearchFormat(record.search_format));
        }

        private void fetchRecordInfo(final View row, final RecordInfo record) {
            RecordLoader.fetch(record, row.getContext(),
                    new RecordLoader.ResponseListener() {
                        @Override
                        public void onMetadataLoaded() {
                            updateBasicMetadataViews(row, record);
                        }
                        @Override
                        public void onSearchFormatLoaded() {
                            updateSearchFormatView(row, record);
                        }
                    });
        }
    }
}
