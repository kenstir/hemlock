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
package org.evergreen_ils.accountAccess.checkout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.opengl.Visibility;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.MaxRenewalsException;
import org.evergreen_ils.accountAccess.ServerErrorMessage;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ItemsCheckOutListView extends ActionBarActivity {

    private final String TAG = ItemsCheckOutListView.class.getName();

    private AccountAccess accountAccess = null;

    private ListView lv;

    private CheckOutArrayAdapter listAdapter = null;

    private ArrayList<CircRecord> circRecords = null;

    private Context context;

    private ProgressDialog progressDialog;

    private TextView itemsNo;

    private TextView overdueItems;

    private Date currentDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.checkout_list);

        // set up action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(AccountAccess.userName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        currentDate = new Date(System.currentTimeMillis());

        context = this;
        itemsNo = (TextView) findViewById(R.id.checkout_items_number);
        overdueItems = (TextView) findViewById(R.id.checkout_items_overdue);
        accountAccess = AccountAccess.getAccountAccess();
        lv = (ListView) findViewById(R.id.checkout_items_list);
        circRecords = new ArrayList<CircRecord>();
        listAdapter = new CheckOutArrayAdapter(context,
                R.layout.checkout_list_item, circRecords);
        lv.setAdapter(listAdapter);

        Thread getCirc = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    circRecords = accountAccess.getItemsCheckedOut();
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(ItemsCheckOutListView.this))
                            circRecords = accountAccess.getItemsCheckedOut();
                    } catch (Exception eauth) {
                        Log.d(TAG, "Exception in reauth", eauth);
                    }
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < circRecords.size(); i++)
                            listAdapter.add(circRecords.get(i));

                        itemsNo.setText(" " + circRecords.size() + " ");

                        // find overdue items
                        int overdueNo = 0;
                        for (int i = 0; i < circRecords.size(); i++) {
                            CircRecord circ = circRecords.get(i);
                            if (circ.getDueDateObject().compareTo(currentDate) < 0)
                                overdueNo++;
                        }
                        overdueItems.setText(" " + overdueNo);

                        progressDialog.dismiss();

                        if (circRecords.size() == 0)
                            Toast.makeText(context, "No records",
                                    Toast.LENGTH_LONG);

                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Retrieving circulation data");
        progressDialog.show();
        getCirc.start();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    class CheckOutArrayAdapter extends ArrayAdapter<CircRecord> {
        private static final String tag = "CheckoutArrayAdapter";

        private TextView recordTitle;
        private TextView recordAuthor;
        private TextView recordDueDate;
        private TextView recordRenewals;
        private TextView renewButton;

        private List<CircRecord> records = new ArrayList<CircRecord>();

        public CheckOutArrayAdapter(Context context, int textViewResourceId,
                List<CircRecord> objects) {
            super(context, textViewResourceId, objects);
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public CircRecord getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            // Get item
            final CircRecord record = getItem(position);

            // if it is the right type of view
            if (row == null) {

                Log.d(tag, "Starting XML Row Inflation ... ");
                LayoutInflater inflater = (LayoutInflater) this
                        .getContext().getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.checkout_list_item, parent,
                        false);
                Log.d(tag, "Successfully completed XML Row Inflation!");

            }

            // Get reference to TextView - title
            recordTitle = (TextView) row.findViewById(R.id.checkout_record_title);

            // Get reference to TextView - author
            recordAuthor = (TextView) row.findViewById(R.id.checkout_record_author);

            // Get reference to TextView - record Publisher date+publisher
            recordDueDate = (TextView) row.findViewById(R.id.checkout_due_date);

            renewButton = (TextView) row.findViewById(R.id.renew_button);
            final boolean renewable = record.getRenewals() > 0;
            renewButton.setVisibility(renewable ? View.VISIBLE : View.GONE);
            renewButton.setEnabled(renewable);
            renewButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!renewable)
                        return;
                    Thread renew = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            boolean refresh = true;
                            AccountAccess ac = AccountAccess
                                    .getAccountAccess();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog = new ProgressDialog(context);
                                    progressDialog.setMessage("Renewing item");
                                    progressDialog.show();
                                }
                            });

                            try {
                                ac.renewCirc(record.getTargetCopy());
                            } catch (MaxRenewalsException e1) {
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        Toast.makeText(context,
                                                "Max renewals reached",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                                refresh = false;
                            } catch (ServerErrorMessage error) {
                                final String errorMessage = error.message;
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        Toast.makeText(context,
                                                errorMessage,
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            } catch (SessionNotFoundException e1) {
                                try {
                                    if (accountAccess.reauthenticate(ItemsCheckOutListView.this))
                                        ac.renewCirc(record.getTargetCopy());
                                } catch (Exception eauth) {
                                    Log.d(TAG, "Exception in reauth", eauth);
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, getString(R.string.item_renewed), Toast.LENGTH_SHORT).show();
                                }
                            });

                            if (refresh) {
                                try {
                                    circRecords = accountAccess.getItemsCheckedOut();
                                } catch (SessionNotFoundException e) {
                                    try {
                                        if (accountAccess.reauthenticate(ItemsCheckOutListView.this))
                                            circRecords = accountAccess.getItemsCheckedOut();
                                    } catch (Exception eauth) {
                                        Log.d(TAG, "Exception in reauth", eauth);
                                    }
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listAdapter.clear();
                                        for (int i = 0; i < circRecords.size(); i++) {
                                            listAdapter.add(circRecords.get(i));
                                        }
                                        progressDialog.dismiss();
                                        listAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    });

                    renew.start();
                }
            });

            // set text
            recordTitle.setText(record.getTitle());
            recordAuthor.setText(record.getAuthor());
            recordDueDate.setText(getString(R.string.due) + ": " + record.getDueDate());
            Log.d(TAG, "title:  " + record.getTitle());
            Log.d(TAG, "author: " + record.getAuthor());
            Log.d(TAG, "due:    " + record.getDueDate());
            Log.d(TAG, "renew:  " + record.getRenewals());

            return row;
        }
    }
}