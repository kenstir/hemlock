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

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.*;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.searchCatalog.RecordDetails;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;
import org.opensrf.util.OSRFObject;
import org.w3c.dom.Text;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static org.evergreen_ils.android.App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT;

public class FinesActivity extends BaseActivity {

    private TextView total_owed;
    private TextView total_paid;
    private TextView balance_owed;
    private Button pay_fines_button;
    private ListView lv;
    private OverdueMaterialsArrayAdapter listAdapter;
    private ArrayList<FinesRecord> finesRecords;
    private boolean haveAnyGroceryBills = false;
    private boolean haveAnyFines = false;
    private Runnable getFinesInfo;
    private AccountAccess ac;
    private ProgressDialogSupport progress;
    private Context context;
    private DecimalFormat decimalFormater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mRestarting) return;

        setContentView(R.layout.activity_fines);

        decimalFormater = new DecimalFormat("#0.00");
        lv = (ListView) findViewById(R.id.fines_overdue_materials_list);

        total_owed = (TextView) findViewById(R.id.fines_total_owed);
        total_paid = (TextView) findViewById(R.id.fines_total_paid);
        balance_owed = (TextView) findViewById(R.id.fines_balance_owed);
        pay_fines_button = (Button) findViewById(R.id.pay_fines);

        context = this;
        ac = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        finesRecords = new ArrayList<>();
        listAdapter = new OverdueMaterialsArrayAdapter(context,
                R.layout.fines_list_item, finesRecords);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<RecordInfo> records = new ArrayList<>();
                if (haveAnyGroceryBills) {
                    // If any of the fines are for non-circulation items ("grocery bills"), we
                    // start the details flow with only the one record, if a record was selected.
                    // The details flow can't handle nulls.
                    RecordInfo record = finesRecords.get(position).recordInfo;
                    if (record != null) {
                        records.add(record);
                        RecordDetails.launchDetailsFlow(FinesActivity.this, records, 0);
                    }
                } else {
                    for (FinesRecord item : finesRecords)
                        records.add(item.recordInfo);
                    RecordDetails.launchDetailsFlow(FinesActivity.this, records, position);
                }
            }
        });

        initPayFinesButton();
        initRunnable();

        progress.show(context, getString(R.string.msg_retrieving_fines));
        new Thread(getFinesInfo).start();
    }

    @Override
    protected void onDestroy() {
        if (progress != null) progress.dismiss();
        super.onDestroy();
    }

    private void initPayFinesButton() {
        Integer home_lib = AccountAccess.getInstance().getHomeLibraryID();
        Organization home_org = EvergreenServer.getInstance().getOrganization(home_lib);
        if (Utils.safeBool(home_org.setting_allow_credit_payments)) {
            pay_fines_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = AccountAccess.getInstance().getUserName();
                    String password = AccountUtils.getPassword(FinesActivity.this, username);
                    String path =                            "/eg/opac/login"
                            + "?redirect_to=" + URLEncoder.encode("/eg/opac/myopac/main_payment_form#pay_fines_now")
                            + "?username=" + URLEncoder.encode(username);
                    if (!TextUtils.isEmpty(password))
                        path = path
                            + "&password=" + URLEncoder.encode(password);
                    String url = EvergreenServer.getInstance().getUrl(path);
                    startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT);
                }
            });
        } else {
            pay_fines_button.setVisibility(View.GONE);
        }
    }

    private float getFloat(OSRFObject o, String field) {
        float ret = 0.0f;
        try {
            ret = Float.parseFloat(o.getString(field));
        } catch (Exception e) {
            Analytics.logException(e);
        }
        return ret;
    }

    private void initRunnable() {
        getFinesInfo = new Runnable() {
            @Override
            public void run() {

                OSRFObject summary = null;
                try {
                    summary = ac.getFinesSummary();
                } catch (SessionNotFoundException e) {
                    try {
                        if (ac.reauthenticate(FinesActivity.this))
                            summary = ac.getFinesSummary();
                    } catch (Exception e1) {
                    }
                }
                final OSRFObject finesSummary = summary;

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
                finesRecords = frecords;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.clear();
                        haveAnyFines = finesRecords.size() > 0;
                        haveAnyGroceryBills = false;
                        for (FinesRecord finesRecord : finesRecords) {
                            listAdapter.add(finesRecord);
                            if (finesRecord.recordInfo == null) {
                                haveAnyGroceryBills = true;
                            }
                        }

                        listAdapter.notifyDataSetChanged();

                        total_owed.setText(decimalFormater.format(getFloat(finesSummary, "total_owed")));
                        total_paid.setText(decimalFormater.format(getFloat(finesSummary, "total_paid")));
                        balance_owed.setText(decimalFormater.format(getFloat(finesSummary, "balance_owed")));
                        progress.dismiss();
                    }
                });
            }
        };
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

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.pay_fines) {
            Toast.makeText(this, "payFines", Toast.LENGTH_LONG).show();
        }
    }
}
