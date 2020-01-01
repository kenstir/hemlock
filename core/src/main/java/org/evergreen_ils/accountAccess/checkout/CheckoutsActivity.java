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
import android.os.Build;
import android.widget.*;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.searchCatalog.RecordDetails;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;
import org.opensrf.util.GatewayResult;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class CheckoutsActivity extends BaseActivity {

    private final static String TAG = CheckoutsActivity.class.getSimpleName();
    private AccountAccess accountAccess = null;
    private ListView lv;
    private CheckOutArrayAdapter listAdapter = null;
    private ArrayList<CircRecord> circRecords = null;
    private ProgressDialogSupport progress;
    private TextView checkoutsSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isRestarting()) return;

        setContentView(R.layout.activity_checkouts);

        checkoutsSummary = findViewById(R.id.checkout_items_summary);
        accountAccess = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        lv = (ListView) findViewById(R.id.checkout_items_list);
        circRecords = new ArrayList<>();
        listAdapter = new CheckOutArrayAdapter(this,
                R.layout.checkout_list_item, circRecords);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<RecordInfo> records = new ArrayList<>();
                for (CircRecord circRecord : circRecords) {
                    if (circRecord.recordInfo.doc_id != -1) {
                        records.add(circRecord.recordInfo);
                    }
                }
                RecordDetails.launchDetailsFlow(CheckoutsActivity.this, records, position);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progress.show(this, getString(R.string.msg_retrieving_data));

        Thread getCirc = initGetCircThread();
        getCirc.start();
    }

    @Override
    protected void onDestroy() {
        if (progress != null) progress.dismiss();
        super.onDestroy();
    }

    private Integer countOverdues() {
        int overdues = 0;
        for (CircRecord circ : circRecords)
            if (circ.isOverdue())
                overdues++;
        return overdues;
    }

    private Thread initGetCircThread() {
        return new Thread(new Runnable() {

                @Override
                public void run() {

                    try {
                        circRecords = accountAccess.getItemsCheckedOut();
                    } catch (SessionNotFoundException e) {
                        try {
                            if (accountAccess.reauthenticate(CheckoutsActivity.this))
                                circRecords = accountAccess.getItemsCheckedOut();
                        } catch (Exception eauth) {
                            Log.d(TAG, "Exception in reauth", eauth);
                        }
                    }
                    Analytics.logEvent("Checkouts: List Checkouts", "num_items", circRecords.size());

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            listAdapter.clear();
                            for (CircRecord circ : circRecords)
                                listAdapter.add(circ);
                            checkoutsSummary.setText(String.format(getString(R.string.checkout_items), circRecords.size()));

                            progress.dismiss();
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
    }

    class CheckOutArrayAdapter extends ArrayAdapter<CircRecord> {
        private TextView recordTitle;
        private TextView recordAuthor;
        private TextView recordFormat;
        private TextView recordRenewals;
        private TextView recordDueDate;
        private TextView recordIsOverdue;
        private TextView renewButton;

        private List<CircRecord> records;

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
            recordTitle = row.findViewById(R.id.checkout_record_title);
            recordAuthor = row.findViewById(R.id.checkout_record_author);
            recordFormat = row.findViewById(R.id.checkout_record_format);
            recordRenewals = row.findViewById(R.id.checkout_record_renewals);
            recordDueDate = row.findViewById(R.id.checkout_record_due_date);
            recordIsOverdue = row.findViewById(R.id.checkout_record_overdue);
            renewButton = row.findViewById(R.id.renew_button);
            initRenewButton(record);

            // set text
            recordTitle.setText(record.getTitle());
            recordAuthor.setText(record.getAuthor());
            recordFormat.setText(RecordInfo.getIconFormatLabel(record.recordInfo));
            recordRenewals.setText(String.format(getString(R.string.checkout_renewals_left), record.getRenewals()));
            recordDueDate.setText(String.format(getString(R.string.due), record.getDueDateString()));
            maybeHighlightDueDate(record);

            return row;
        }

        private void maybeHighlightDueDate(final CircRecord record) {
            recordIsOverdue.setVisibility(record.isOverdue() ? View.VISIBLE : View.GONE);
            int style = (record.isDue() ? R.style.alertText : R.style.HemlockText_ListTertiary);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                recordDueDate.setTextAppearance(style);
            } else {
                recordDueDate.setTextAppearance(getApplicationContext(), style);
            }
        }

        private void initRenewButton(final CircRecord record) {
            final boolean renewable = record.getRenewals() > 0;
            renewButton.setEnabled(renewable);
            renewButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!renewable)
                        return;
                    AlertDialog.Builder builder = new AlertDialog.Builder(CheckoutsActivity.this);
                    builder.setMessage(R.string.renew_item_message);
                    builder.setNegativeButton(android.R.string.no, null);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Analytics.logEvent("Checkouts: Renew", "num_renewals", record.getRenewals(), "overdue", record.isOverdue());
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.show(CheckoutsActivity.this, getString(R.string.msg_renewing_item));
                    }
                });

                AccountAccess ac = AccountAccess.getInstance();
                GatewayResult resp = null;
                Exception ex = null;
                try {
                    resp = ac.renewCirc(record.getTargetCopy());
                } catch (SessionNotFoundException e1) {
                    try {
                        if (accountAccess.reauthenticate(CheckoutsActivity.this)) {
                            resp = ac.renewCirc(record.getTargetCopy());
                        }
                    } catch (Exception eauth) {
                        ex = eauth;
                    }
                }
                if (resp == null || resp.failed) {
                    final String msg;
                    if (ex != null) {
                        msg = ex.getMessage();
                    } else if (resp != null) {
                        msg = resp.errorMessage;
                    } else {
                        msg = "Unexpected error";
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(CheckoutsActivity.this);
                            builder.setTitle("Failed to renew item")
                                    .setMessage(msg)
                                    .setPositiveButton(android.R.string.ok, null);
                            builder.create().show();                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CheckoutsActivity.this, getString(R.string.toast_item_renewed), Toast.LENGTH_LONG).show();
                        }
                    });
                    try {
                        circRecords = accountAccess.getItemsCheckedOut();
                    } catch (SessionNotFoundException e) {
                        try {
                            if (accountAccess.reauthenticate(CheckoutsActivity.this))
                                circRecords = accountAccess.getItemsCheckedOut();
                        } catch (Exception eauth) {
                            Log.d(TAG, "Exception in reauth", eauth);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listAdapter.clear();
                            for (CircRecord circ : circRecords)
                                listAdapter.add(circ);

                            progress.dismiss();
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        renew.start();
    }
}
