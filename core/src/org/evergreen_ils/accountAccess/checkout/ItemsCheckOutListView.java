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
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.widget.*;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.MaxRenewalsException;
import org.evergreen_ils.accountAccess.ServerErrorMessage;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.RecordDetails;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class ItemsCheckOutListView extends ActionBarActivity {

    private final static String TAG = ItemsCheckOutListView.class.getSimpleName();
    private AccountAccess accountAccess = null;
    private ListView lv;
    private CheckOutArrayAdapter listAdapter = null;
    private ArrayList<CircRecord> circRecords = null;
    private Context context;
    private ProgressDialog progressDialog;
    private TextView itemsNo;
    private TextView overdueItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }
        SearchFormat.init(this);

        setContentView(R.layout.checkout_list);
        ActionBarUtils.initActionBarForActivity(this);

        context = this;
        itemsNo = (TextView) findViewById(R.id.checkout_items_number);
        overdueItems = (TextView) findViewById(R.id.checkout_items_overdue);
        accountAccess = AccountAccess.getInstance();
        lv = (ListView) findViewById(R.id.checkout_items_list);
        circRecords = new ArrayList<CircRecord>();
        listAdapter = new CheckOutArrayAdapter(context,
                R.layout.checkout_list_item, circRecords);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RecordInfo info = circRecords.get(position).recordInfo;
                RecordDetails.launchDetailsFlow(ItemsCheckOutListView.this, info);
            }
        });

        Thread getCirc = initGetCircThread();

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.msg_retrieving_data));
        progressDialog.show();
        getCirc.start();
    }

    private Thread initGetCircThread() {
        return new Thread(new Runnable() {

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
                            int overdueNo = 0;
                            for (int i = 0; i < circRecords.size(); i++) {
                                CircRecord circ = circRecords.get(i);
                                listAdapter.add(circ);
                                if (circ.isOverdue()) {
                                    overdueNo++;
                                }
                            }
                            itemsNo.setText(" " + circRecords.size() + " ");
                            overdueItems.setText(" " + overdueNo);

                            progressDialog.dismiss();

                            if (circRecords.size() == 0)
                                Toast.makeText(context, "No records", Toast.LENGTH_LONG).show();

                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    class CheckOutArrayAdapter extends ArrayAdapter<CircRecord> {
        private TextView recordTitle;
        private TextView recordAuthor;
        private TextView recordFormat;
        private TextView recordDueDate;
        private TextView recordIsOverdue;
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
                LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.checkout_list_item, parent, false);
            }

            // Get references to views
            recordTitle = (TextView) row.findViewById(R.id.checkout_record_title);
            recordAuthor = (TextView) row.findViewById(R.id.checkout_record_author);
            recordFormat = (TextView) row.findViewById(R.id.checkout_record_format);
            recordDueDate = (TextView) row.findViewById(R.id.checkout_record_due_date);
            recordIsOverdue = (TextView) row.findViewById(R.id.checkout_record_overdue);
            renewButton = (TextView) row.findViewById(R.id.renew_button);
            initRenewButton(record);

            // set text
            recordTitle.setText(record.getTitle());
            recordAuthor.setText(record.getAuthor());
            recordFormat.setText(RecordInfo.getFormatLabel(record.recordInfo));
            recordDueDate.setText(getString(R.string.due) + " " + record.getDueDate());
            recordIsOverdue.setVisibility(record.isOverdue() ? View.VISIBLE : View.GONE);
            Log.d(TAG, "title: \"" + record.getTitle() + "\""
                    + " due: " + record.getDueDate()
                    + " renewals:  " + record.getRenewals());

            return row;
        }

        private void initRenewButton(final CircRecord record) {
            final boolean renewable = record.getRenewals() > 0;// || GlobalConfigs.isDebuggable();
            renewButton.setEnabled(renewable);
            renewButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!renewable)
                        return;
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.renew_dialog_message);
                    builder.setNegativeButton(android.R.string.no, null);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            renewItem(record);
                        }
                    });
                    builder.create().show();
                }
            });
        }
    }

    private void renewItem(final CircRecord record) {
        Thread renew = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                AccountAccess ac = AccountAccess.getInstance();

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
                    success = true;
                } catch (SessionNotFoundException e1) {
                    try {
                        if (accountAccess.reauthenticate(ItemsCheckOutListView.this)) {
                            ac.renewCirc(record.getTargetCopy());
                            success = true;
                        }
                    } catch (Exception eauth) {
                        Log.d(TAG, "Exception in reauth", eauth);
                    }
                } catch (MaxRenewalsException e1) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(context, R.string.toast_max_renewals_reached, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final ServerErrorMessage error) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                if (success) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, getString(R.string.toast_item_renewed), Toast.LENGTH_LONG).show();
                        }
                    });
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
}
