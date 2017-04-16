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
package org.evergreen_ils.accountAccess.fines;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.AppCompatActivity;
import android.widget.*;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.searchCatalog.RecordDetails;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FinesActivity extends BaseActivity {

    private TextView total_owned;

    private TextView total_paid;

    private TextView balance_owed;

    private ListView lv;

    private Runnable getFinesInfo;

    private AccountAccess ac;

    private ProgressDialogSupport progress;

    private OverdueMaterialsArrayAdapter listAdapter;

    private TextView headerTitle;

    private Context context;
    
    private DecimalFormat decimalFormater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fines);

        decimalFormater = new DecimalFormat("#0.00");
        lv = (ListView) findViewById(R.id.fines_overdue_materials_list);

        total_owned = (TextView) findViewById(R.id.fines_total_owed);
        total_paid = (TextView) findViewById(R.id.fines_total_paid);
        balance_owed = (TextView) findViewById(R.id.fined_balance_owed);

        context = this;
        ac = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        final ArrayList<FinesRecord> finesRecords = new ArrayList<>();
        listAdapter = new OverdueMaterialsArrayAdapter(context,
                R.layout.fines_list_item, finesRecords);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<RecordInfo> records = new ArrayList<>();
                for (FinesRecord item: finesRecords) {
                    records.add(item.recordInfo);
                }
                RecordDetails.launchDetailsFlow(FinesActivity.this, records, position);
            }
        });

        progress.show(context, getString(R.string.msg_retrieving_fines));

        getFinesInfo = new Runnable() {
            @Override
            public void run() {

                float[] finesR = null;
                try {
                    finesR = ac.getFinesSummary();
                } catch (SessionNotFoundException e) {
                    try {
                        if (ac.reauthenticate(FinesActivity.this))
                            finesR = ac.getFinesSummary();
                    } catch (Exception e1) {
                    }
                }

                ArrayList<FinesRecord> frecords = null;
                try {
                    frecords = ac.getTransactions();
                } catch (SessionNotFoundException e) {
                    try {
                        if (ac.reauthenticate(FinesActivity.this))
                            frecords = ac.getTransactions();
                    } catch (Exception e1) {
                    }
                }

                final ArrayList<FinesRecord> finesRecords = frecords;
                final float[] fines = finesR;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.clear();
                        for (FinesRecord finesRecord : finesRecords)
                            listAdapter.add(finesRecord);

                        listAdapter.notifyDataSetChanged();

                        total_owned.setText(decimalFormater.format(fines[0]));
                        total_paid.setText(decimalFormater.format(fines[1]));
                        balance_owed.setText(decimalFormater.format(fines[2]));
                        progress.dismiss();
                    }
                });
            }
        };

        Thread getFinesTh = new Thread(getFinesInfo);
        getFinesTh.start();
    }

    @Override
    protected void onDestroy() {
        if (progress != null) progress.dismiss();
        super.onDestroy();
    }

    class OverdueMaterialsArrayAdapter extends ArrayAdapter<FinesRecord> {
        private static final String tag = "CheckoutArrayAdapter";

        private TextView fineTitle;
        private TextView fineAuthor;
        private TextView fineBalanceOwed;
        private TextView fineStatus;

        private List<FinesRecord> records = new ArrayList<>();

        OverdueMaterialsArrayAdapter(Context context,
                                     int textViewResourceId, List<FinesRecord> objects) {
            super(context, textViewResourceId, objects);
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public FinesRecord getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            // Get item
            final FinesRecord record = getItem(position);

            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.fines_list_item, parent, false);
            }

            fineTitle = (TextView) row.findViewById(R.id.fines_title);
            fineAuthor = (TextView) row.findViewById(R.id.fines_author);
            fineBalanceOwed = (TextView) row.findViewById(R.id.fines_balance_owed);
            fineStatus = (TextView) row.findViewById(R.id.fines_status);

            fineTitle.setText(record.title);
            fineAuthor.setText(record.author);
            fineBalanceOwed.setText(decimalFormater.format(record.balance_owed));
            fineStatus.setText(record.getStatus());

            return row;
        }
    }
}
