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

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.accountAccess.bookbags.BookBag;
import org.evergreen_ils.accountAccess.holds.PlaceHold;
import org.evergreen_ils.barcodescan.CaptureActivity;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.CompatSpinnerAdapter;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SearchCatalogListView extends ActionBarActivity {

    private final String TAG = SearchCatalogListView.class.getSimpleName();

    private ArrayList<RecordInfo> recordList;

    private EditText searchText;

    private ImageButton searchButton;

    private SearchCatalog search;

    private ListView lv;

    private SearchArrayAdapter adapter;

    private Context context;

    private ProgressDialog progressDialog;

    private ArrayList<RecordInfo> searchResults;

    private Spinner choseOrganisation;

    private GlobalConfigs globalConfigs;

    private static final int PLACE_HOLD = 0;

    private static final int DETAILS = 1;

    private static final int BOOK_BAG = 2;

    private TextView searchResultsNumber;

    private ArrayList<BookBag> bookBags;

    private Integer bookbag_selected = -1;

    private final ImageDownloader imageDownloader = new ImageDownloader();

    private Runnable searchForResultsRunnable = null;

    private View searchOptionsMenu = null;
    private Spinner searchClassSpinner;
    private Spinner searchFormatSpinner;

    // marks when the fetching record thread is started
    private boolean loadingElements = false;

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

        setContentView(R.layout.search_result_list);
        ActionBarUtils.initActionBarForActivity(this);

        // get bookbags
        bookBags = AccountAccess.getAccountAccess().getBookbags();

        // singleton initialize necessary IDL and Org data
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);

        context = this;
        search = SearchCatalog.getInstance();

        recordList = new ArrayList<RecordInfo>();

        // Create a customized ArrayAdapter
        adapter = new SearchArrayAdapter(getApplicationContext(),
                R.layout.search_result_item, recordList);

        //searchOptionsMenu = findViewById(R.id.search_preference_options);
        searchClassSpinner = (Spinner) findViewById(R.id.search_class_spinner);
        searchFormatSpinner = (Spinner) findViewById(R.id.search_format_spinner);
        searchResultsNumber = (TextView) findViewById(R.id.search_result_number);
        initSearchFormatSpinner();

        // Get reference to ListView holder
        lv = (ListView) this.findViewById(R.id.search_results_list);

        progressDialog = new ProgressDialog(context);

        // Set the ListView adapter
        lv.setAdapter(adapter);

        searchResults = new ArrayList<RecordInfo>();

        registerForContextMenu(lv);

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

                        //searchOptionsMenu.setVisibility(View.GONE);
                        searchResultsNumber.setVisibility(View.VISIBLE);

                        progressDialog = ProgressDialog.show(
                                context,
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

                            // add extra record to display more option button
                            /*
                             * if (search.visible > recordList.size()) {
                             * recordList.add(new RecordInfo());
                             * searchResultsNumber.setText(+recordList.size() -
                             * 1 + " out of " + search.visible); } else
                             */
                        }
                        searchResultsNumber.setText(+recordList.size()
                                + " out of " + search.visible);

                        adapter.notifyDataSetChanged();
                        progressDialog.dismiss();

                    }
                });

            }
        };

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {

                RecordInfo info = (RecordInfo) lv.getItemAtPosition(position);

                if (info.dummy == true) {
                    // this is the more view item button
                    progressDialog = new ProgressDialog(context);

                    progressDialog.setMessage("Fetching data");
                    progressDialog.show();
                    final String text = searchText.getText().toString();

                    Thread searchThreadwithOffset = new Thread(new Runnable() {

                        @Override
                        public void run() {

                            searchResults.clear();

                            searchResults = search.getSearchResults(text, getSearchClass(), getSearchFormat(), recordList.size());

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    // don't clear record list
                                    // recordList.clear();
                                    if (searchResults.size() > 0) {

                                        // remove previous more button
                                        recordList.remove(recordList.size() - 1);

                                        for (int j = 0; j < searchResults
                                                .size(); j++)
                                            recordList.add(searchResults.get(j));

                                        // add extra record to display more
                                        // option button
                                        if (search.visible > recordList.size()) {
                                            recordList.add(new RecordInfo());
                                            searchResultsNumber.setText(adapter
                                                    .getCount()
                                                    - 1
                                                    + " out of "
                                                    + search.visible);
                                        } else
                                            searchResultsNumber.setText(adapter
                                                    .getCount()
                                                    + " out of "
                                                    + search.visible);
                                    } else {
                                        searchResultsNumber.setText(adapter
                                                .getCount()
                                                + " out of "
                                                + search.visible);
                                    }
                                    adapter.notifyDataSetChanged();
                                    progressDialog.dismiss();
                                }
                            });

                        }
                    });

                    searchThreadwithOffset.start();
                } else {
                    // start activity with book details

                    Intent intent = new Intent(getBaseContext(), SampleUnderlinesNoFade.class);
                    // serialize object and pass it to next activity
                    intent.putExtra("recordInfo", info);
                    intent.putExtra("orgID", search.selectedOrganization.id);
                    intent.putExtra("depth", (search.selectedOrganization.level - 1));
                    intent.putExtra("recordList", recordList);
                    intent.putExtra("recordPosition", position);
                    startActivityForResult(intent, 10);
                }
            }
        });

        lv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

                if (!loadingElements) {

                    /*
                    Log.d(TAG, " Scroll adapter " + totalItemCount + " "
                            + visibleItemCount + " " + firstVisibleItem + " "
                            + adapter.getCount() + " " + search.visible);
                            */
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

        searchText = (EditText) findViewById(R.id.searchText);

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


        searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Thread searchThread = new Thread(searchForResultsRunnable);
                searchThread.start();
            }
        });

        //kenstir todo: factor this out
        int selectedOrgPos = 0;
        int homeLibrary = 0;
        if (AccountAccess.getAccountAccess() != null) {
            homeLibrary = AccountAccess.getAccountAccess().getHomeLibraryID();
        }
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < globalConfigs.organisations.size(); i++) {
            Organisation org = globalConfigs.organisations.get(i);
            list.add(org.padding + org.name);
            if (org.id == homeLibrary) {
                selectedOrgPos = i;
            }
        }
        ArrayAdapter<String> adapter = CompatSpinnerAdapter.CreateCompatSpinnerAdapter(this, list);
        choseOrganisation = (Spinner) findViewById(R.id.chose_organisation);
        choseOrganisation.setAdapter(adapter);
        choseOrganisation.setSelection(selectedOrgPos);
        choseOrganisation.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int ID, long arg3) {
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
            intent.putExtra("depth", (search.selectedOrganization.level - 1));

            intent.putExtra("recordList", recordList);
            // TODO put total number
            intent.putExtra("recordPosition", menuArrayItem.position);
            startActivity(intent);
        }
            break;
        case PLACE_HOLD: {

            Intent intent = new Intent(getBaseContext(), PlaceHold.class);

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
                        // TODO Auto-generated method stub
                        Thread addtoBookbag = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                AccountAccess ac = AccountAccess.getAccountAccess();
                                try {
                                    ac.addRecordToBookBag(info.doc_id,
                                            bookBags.get(bookbag_selected).id);
                                } catch (SessionNotFoundException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_advanced_search) {
            startActivityForResult(new Intent(getApplicationContext(), AdvancedSearchActivity.class), 2);
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

        private static final String tag = "SearchArrayAdapter";
        private Context context;
        private ImageView recordImage;
        private TextView recordTitle;
        private TextView recordAuthor;
        private TextView recordFormat;
        private TextView recordPublisher;

        private List<RecordInfo> records = new ArrayList<RecordInfo>();

        public SearchArrayAdapter(Context context, int textViewResourceId,
                List<RecordInfo> objects) {
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

            // Get item
            RecordInfo record = getItem(position);

            // if it is the right type of view
            if (row == null || row.findViewById(R.id.search_record_title) == null) {
                LayoutInflater inflater = (LayoutInflater) this
                        .getContext().getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.search_result_item, parent,
                        false);
            }

            // Get reference to ImageView
            recordImage = (ImageView) row.findViewById(R.id.search_record_img);
            String imageHref = GlobalConfigs.getUrl("/opac/extras/ac/jacket/small/r/" + record.image);
            //Log.d(TAG, "image url " + imageHref);

            // start async download of image
            imageDownloader.download(imageHref, recordImage);

            recordTitle = (TextView) row.findViewById(R.id.search_record_title);
            recordAuthor = (TextView) row.findViewById(R.id.search_record_author);
            recordFormat = (TextView) row.findViewById(R.id.search_record_format);
            recordPublisher = (TextView) row.findViewById(R.id.search_record_publishing);

            // set text
            recordTitle.setText(record.title);
            recordAuthor.setText(record.author);
            recordFormat.setText(SearchFormat.getItemLabelFromSearchFormat(record.search_format));
            recordPublisher.setText(record.pubdate + " " + record.publisher);

            return row;
        }
    }
}
