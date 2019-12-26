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

import android.app.AlertDialog;
import android.view.WindowManager;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class BookBagActivity extends BaseActivity {

    private final static String TAG = BookBagActivity.class.getSimpleName();

    private AccountAccess accountAccess = null;

    private ListView lv;

    private BookBagsArrayAdapter listAdapter = null;

    private ArrayList<BookBag> bookBags = null;

    private ProgressDialogSupport progress;

    private EditText bookbag_name;

    private Button create_bookbag;

    private Runnable getBookbagsRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mRestarting) return;

        setContentView(R.layout.activity_bookbags);

        // prevent soft keyboard from popping up when the activity starts
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        accountAccess = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        bookbag_name = findViewById(R.id.bookbag_create_name);
        create_bookbag = findViewById(R.id.bookbag_create_button);
        create_bookbag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createBookbag(bookbag_name.getText().toString());
            }
        });

        lv = findViewById(R.id.bookbag_list);
        bookBags = new ArrayList<BookBag>();
        listAdapter = new BookBagsArrayAdapter(this, R.layout.bookbag_list_item, bookBags);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Analytics.logEvent("Lists: Tap List");
                BookBag item = (BookBag) lv.getItemAtPosition(position);
                Intent intent = new Intent(BookBagActivity.this, BookBagDetailsActivity.class);
                intent.putExtra("bookBag", item);
                startActivityForResult(intent, 0);
            }
        });

        initGetBookbagsRunnable();

        new Thread(getBookbagsRunnable).start();
    }

    @Override
    protected void onDestroy() {
        if (progress != null) progress.dismiss();
        super.onDestroy();
    }

    private void initGetBookbagsRunnable() {
        getBookbagsRunnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) progress.show(BookBagActivity.this, getString(R.string.msg_retrieving_lists));
                    }
                });

                try {
                    accountAccess.retrieveBookbags();
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(BookBagActivity.this))
                            accountAccess.retrieveBookbags();
                    } catch (Exception e2) {
                        Log.d(TAG, "caught", e2);
                    }
                }
                bookBags = accountAccess.getBookbags();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.clear();
                        for (BookBag bookBag : bookBags)
                            listAdapter.add(bookBag);

                        listAdapter.notifyDataSetChanged();

                        progress.dismiss();
                    }
                });
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
        case BookBagDetailsActivity.RESULT_CODE_UPDATE:
            new Thread(getBookbagsRunnable).start();
            break;
        }
    }

    private void createBookbag(final String name) {
        if (name.length() < 2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.msg_list_name_too_short_title)
                    .setMessage(R.string.msg_list_name_too_short)
                    .setPositiveButton(android.R.string.ok, null);
            builder.create().show();
            return;
        }
        Analytics.logEvent("Lists: Create List");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    accountAccess.createBookbag(name);
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(BookBagActivity.this))
                            accountAccess.createBookbag(name);
                    } catch (Exception eauth) {
                        Log.d(TAG, "caught", eauth);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                    }
                });

                new Thread(getBookbagsRunnable).start();
            }
        });

        progress.show(this, getString(R.string.msg_creating_list));
        thread.start();
    }

    class BookBagsArrayAdapter extends ArrayAdapter<BookBag> {
        private List<BookBag> records;

        public BookBagsArrayAdapter(Context context, int textViewResourceId, List<BookBag> objects) {
            super(context, textViewResourceId, objects);
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public BookBag getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            // Get item
            final BookBag record = getItem(position);

            // if it is the right type of view
            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.bookbag_list_item, parent, false);
            }

            TextView nameText = (TextView) row.findViewById(R.id.bookbag_name);
            nameText.setText(Utils.safeString(record.name));
            TextView descText = (TextView) row.findViewById(R.id.bookbag_description);
            descText.setText(Utils.safeString(record.description));
            TextView itemsText = (TextView) row.findViewById(R.id.bookbag_items);
            itemsText.setText(getResources().getQuantityString(R.plurals.number_of_items,
                    record.items.size(), record.items.size()));

            return row;
        }
    }
}
