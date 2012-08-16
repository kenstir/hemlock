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
package org.evergreen.android.accountAccess.fines;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.AccountScreenDashboard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class FinesActivity extends Activity {

    private TextView total_owned;

    private TextView total_paid;

    private TextView balance_owed;

    private ListView lv;

    private Runnable getFinesInfo;

    private AccountAccess ac;

    private ProgressDialog progressDialog;

    private OverdueMaterialsArrayAdapter listAdapter;

    private Button homeButton;

    private Button myAccountButton;

    private TextView headerTitle;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fines);

        // header portion actions
        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.fines_title);

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

        lv = (ListView) findViewById(R.id.fines_overdue_materials_list);

        total_owned = (TextView) findViewById(R.id.fines_total_owned);
        total_paid = (TextView) findViewById(R.id.fines_total_paid);
        balance_owed = (TextView) findViewById(R.id.fined_balance_owed);
        context = this;

        ac = AccountAccess.getAccountAccess();

        ArrayList<FinesRecord> finesRecords = new ArrayList<FinesRecord>();
        listAdapter = new OverdueMaterialsArrayAdapter(context,
                R.layout.fines_list_item, finesRecords);
        lv.setAdapter(listAdapter);

        progressDialog = ProgressDialog.show(this, null, "Retrieving fines");

        getFinesInfo = new Runnable() {
            @Override
            public void run() {

                float[] finesR = null;
                try {
                    finesR = ac.getFinesSummary();
                } catch (SessionNotFoundException e) {
                    try {
                        if (ac.authenticate())
                            finesR = ac.getFinesSummary();
                    } catch (Exception e1) {
                    }
                } catch (NoNetworkAccessException e) {
                    Utils.showNetworkNotAvailableDialog(context);
                } catch (NoAccessToServer e) {
                    Utils.showServerNotAvailableDialog(context);
                }

                ArrayList<FinesRecord> frecords = null;
                try {
                    frecords = ac.getTransactions();
                } catch (SessionNotFoundException e) {

                    try {
                        if (ac.authenticate())
                            frecords = ac.getTransactions();
                    } catch (Exception e1) {
                    }

                } catch (NoNetworkAccessException e) {
                    Utils.showNetworkNotAvailableDialog(context);
                } catch (NoAccessToServer e) {
                    Utils.showServerNotAvailableDialog(context);
                }

                final ArrayList<FinesRecord> finesRecords = frecords;
                final float[] fines = finesR;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        listAdapter.clear();

                        for (int i = 0; i < finesRecords.size(); i++)
                            listAdapter.add(finesRecords.get(i));

                        listAdapter.notifyDataSetChanged();

                        total_owned.setText(fines[0] + "");
                        total_paid.setText(fines[1] + "");
                        balance_owed.setText(fines[2] + "");
                        progressDialog.dismiss();
                    }
                });
            }
        };

        Thread getFinesTh = new Thread(getFinesInfo);
        getFinesTh.start();
    }

    class OverdueMaterialsArrayAdapter extends ArrayAdapter<FinesRecord> {
        private static final String tag = "CheckoutArrayAdapter";

        private TextView fineTitle;
        private TextView fineAuthor;
        private TextView fineBalanceOwed;
        private TextView fineStatus;

        private List<FinesRecord> records = new ArrayList<FinesRecord>();

        public OverdueMaterialsArrayAdapter(Context context,
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

                Log.d(tag, "Starting XML view more infaltion ... ");
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.fines_list_item, parent, false);
                Log.d(tag, "Successfully completed XML view more Inflation!");

            }
            // Get reference to TextView - title
            fineTitle = (TextView) row.findViewById(R.id.fines_title);

            // Get reference to TextView author
            fineAuthor = (TextView) row.findViewById(R.id.fines_author);

            // Get hold status
            fineBalanceOwed = (TextView) row
                    .findViewById(R.id.fines_balance_owed);

            fineStatus = (TextView) row.findViewById(R.id.fines_status);
            // set text

            // set raw information
            fineTitle.setText(record.title);
            fineAuthor.setText(record.author);
            fineBalanceOwed.setText(record.balance_owed);
            // status.setText(record.getHoldStatus());
            fineStatus.setText(record.getStatus());

            if (record.getStatus().equals("returned")) {
                fineStatus.setTextColor(Color.argb(255, 0, 255, 0));
            } else
                fineStatus.setTextColor(Color.argb(255, 255, 0, 0));

            return row;
        }
    }
}