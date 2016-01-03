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
package org.evergreen_ils.accountAccess.bookbags;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.SearchCatalog;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BookBagDetails extends ActionBarActivity {

    private final static String TAG = BookBagDetails.class.getSimpleName();

    public static final int RESULT_CODE_UPDATE = 1;

    private SearchCatalog search;

    private AccountAccess accountAccess;

    private ListView lv;

    private BookBagItemsArrayAdapter listAdapter = null;

    private ArrayList<BookBagItem> bookBagItems = null;

    private Context context;

    private ProgressDialog progressDialog;

    private BookBag bookBag;

    private TextView bookbag_name;

    private Button delete_bookbag_button;

    private Runnable getBookBagsItemsRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.bookbagitem_list);
        ActionBarUtils.initActionBarForActivity(this);

        accountAccess = AccountAccess.getInstance();
        bookBag = (BookBag) getIntent().getSerializableExtra("bookBag");

        context = this;
        search = SearchCatalog.getInstance();
        bookbag_name = (TextView) findViewById(R.id.bookbag_name);
        delete_bookbag_button = (Button) findViewById(R.id.remove_bookbag);
        bookbag_name.setText(bookBag.name);
        delete_bookbag_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                final Thread deleteBookbag = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            accountAccess.deleteBookBag(bookBag.id);
                        } catch (SessionNotFoundException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                setResult(RESULT_CODE_UPDATE);
                                finish();
                            }
                        });
                    }
                });

                Builder confirmationDialogBuilder = new AlertDialog.Builder(
                        context);
                confirmationDialogBuilder.setMessage("Delete bookbag?");

                confirmationDialogBuilder.setNegativeButton(
                        android.R.string.no, null);
                confirmationDialogBuilder.setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                progressDialog = ProgressDialog.show(context,
                                        getResources().getText(R.string.dialog_please_wait),
                                        "Deleting Bookbag");
                                deleteBookbag.start();
                            }
                        });

                confirmationDialogBuilder.create().show();

            }
        });

        lv = (ListView) findViewById(R.id.bookbagitem_list);
        bookBagItems = new ArrayList<BookBagItem>();
        listAdapter = new BookBagItemsArrayAdapter(context,
                R.layout.bookbagitem_list_item, bookBagItems);
        lv.setAdapter(listAdapter);

        lv.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        getBookBagsItemsRunnable = new Runnable() {

            @Override
            public void run() {
                ArrayList<Integer> ids = new ArrayList<Integer>();

                for (int i = 0; i < bookBag.items.size(); i++) {
                    ids.add(bookBag.items.get(i).target_copy);
                }
                ArrayList<RecordInfo> records = search.getRecordsInfo(ids);

                for (int i = 0; i < bookBag.items.size(); i++) {
                    bookBag.items.get(i).recordInfo = records.get(i);
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        listAdapter.clear();

                        for (int i = 0; i < bookBag.items.size(); i++)
                            listAdapter.add(bookBag.items.get(i));

                        progressDialog.dismiss();

                        if (bookBagItems.size() == 0)
                            Toast.makeText(context, "No bookbags", Toast.LENGTH_LONG).show();

                        listAdapter.notifyDataSetChanged();
                    }
                });

            }
        };

        Thread getBookBags = new Thread(getBookBagsItemsRunnable);

        progressDialog = ProgressDialog.show(context,
                getResources().getText(R.string.dialog_please_wait),
                "Retrieving bookbag data");
        getBookBags.start();

    }

    class BookBagItemsArrayAdapter extends ArrayAdapter<BookBagItem> {
        private static final String tag = "BookbagArrayAdapter";

        private TextView title;
        private TextView author;
        private Button remove;

        private List<BookBagItem> records = new ArrayList<BookBagItem>();

        public BookBagItemsArrayAdapter(Context context,
                int textViewResourceId, List<BookBagItem> objects) {
            super(context, textViewResourceId, objects);
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public BookBagItem getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            // Get item
            final BookBagItem record = getItem(position);

            // if it is the right type of view
            if (row == null) {

                Log.d(tag, "Starting XML Row Inflation ... ");
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.bookbagitem_list_item, parent,
                        false);
                Log.d(tag, "Successfully completed XML Row Inflation!");

            }

            title = (TextView) row.findViewById(R.id.bookbagitem_title);

            author = (TextView) row.findViewById(R.id.bookbagitem_author);

            remove = (Button) row.findViewById(R.id.bookbagitem_remove_button);

            title.setText(record.recordInfo.title);

            author.setText(record.recordInfo.author);

            remove.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    Thread removeItem = new Thread(new Runnable() {

                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog = ProgressDialog.show(context,
                                            getResources().getText(R.string.dialog_please_wait),
                                            "Removing item");
                                }
                            });

                            try {
                                accountAccess.removeBookbagItem(record.id);
                            } catch (SessionNotFoundException e) {
                                try {
                                    if (accountAccess.reauthenticate(BookBagDetails.this))
                                        accountAccess.removeBookbagItem(record.id);
                                } catch (Exception e1) {
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();

                                    Thread getBookBags = new Thread(
                                            getBookBagsItemsRunnable);
                                    setResult(RESULT_CODE_UPDATE);

                                    bookBag.items.remove(record);
                                    progressDialog = ProgressDialog.show(context,
                                            getResources().getText(R.string.dialog_please_wait),
                                            "Retrieving bookbag data");
                                    getBookBags.start();

                                }
                            });
                        }
                    });

                    removeItem.start();
                }
            });

            return row;
        }
    }
}
