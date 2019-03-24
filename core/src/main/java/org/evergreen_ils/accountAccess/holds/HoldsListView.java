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
package org.evergreen_ils.accountAccess.holds;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;

import java.util.ArrayList;
import java.util.List;

public class HoldsListView extends BaseActivity {

    private final static String TAG = HoldsListView.class.getSimpleName();

    private AccountAccess accountAccess = null;
    private ListView lv;
    private HoldsArrayAdapter listAdapter = null;
    private List<HoldRecord> holdRecords = null;
    private Context context;
    private Runnable getHoldsRunnable = null;
    private TextView holdsNoText;
    private ProgressDialogSupport progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mRestarting) return;

        setContentView(R.layout.activity_holds);

        holdsNoText = findViewById(R.id.holds_number);
        lv = findViewById(R.id.holds_item_list);
        context = this;
        accountAccess = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        holdRecords = new ArrayList<>();
        listAdapter = new HoldsArrayAdapter(context, R.layout.holds_list_item, holdRecords);
        lv.setAdapter(listAdapter);

        getHoldsRunnable = new Runnable() {
            @Override
            public void run() {

                try {
                    holdRecords = accountAccess.getHolds();
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(HoldsListView.this))
                            holdRecords = accountAccess.getHolds();
                    } catch (Exception eauth) {
                        Log.d(TAG, "Exception in reauth");
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.clear();
                        for (int i = 0; i < holdRecords.size(); i++)
                            listAdapter.add(holdRecords.get(i));

                        holdsNoText.setText(String.format("%d", listAdapter.getCount()));
                        progress.dismiss();
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        progress.show(context, getString(R.string.msg_loading_holds));
        new Thread(getHoldsRunnable).start();

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Analytics.logEvent("Holds: Tap List Item");
                HoldRecord record = (HoldRecord) lv.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), HoldDetails.class);
                intent.putExtra("holdRecord", record);

                // request code does not matter, but we use the result code
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (progress != null) progress.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {

        case HoldDetails.RESULT_CODE_CANCEL:
            Log.d(TAG, "Do nothing");
            break;

        case HoldDetails.RESULT_CODE_DELETE_HOLD:
        case HoldDetails.RESULT_CODE_UPDATE_HOLD:
            progress.show(context, getString(R.string.msg_loading_holds));
            new Thread(getHoldsRunnable).start();
            Log.d(TAG, "Update on result "+resultCode);
            break;
        }
    }

    class HoldsArrayAdapter extends ArrayAdapter<HoldRecord> {
        private final String tag = HoldsArrayAdapter.class.getSimpleName();

        private TextView holdTitle;
        private TextView holdAuthor;
        private TextView holdFormat;
        private TextView status;

        private List<HoldRecord> records = new ArrayList<HoldRecord>();

        public HoldsArrayAdapter(Context context, int textViewResourceId,
                List<HoldRecord> objects) {
            super(context, textViewResourceId, objects);
            this.records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public HoldRecord getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            final HoldRecord record = getItem(position);

            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.holds_list_item, parent, false);
            }

            holdTitle = (TextView) row.findViewById(R.id.hold_title);
            holdAuthor = (TextView) row.findViewById(R.id.hold_author);
            holdFormat = (TextView) row.findViewById(R.id.hold_format);
            status = (TextView) row.findViewById(R.id.hold_status);

            holdTitle.setText(record.title);
            holdAuthor.setText(record.author);
            holdFormat.setText(RecordInfo.getFormatLabel(record.recordInfo));
            status.setText(record.getHoldStatus(getResources()));

            return row;
        }
    }
}
