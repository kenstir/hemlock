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
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.ProgressBarSupport;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.util.ArrayList;
import java.util.List;

public class HoldsListView extends ActionBarActivity {

    private final static String TAG = HoldsListView.class.getSimpleName();

    private AccountAccess accountAccess = null;
    private ListView lv;
    private HoldsArrayAdapter listAdapter = null;
    private List<HoldRecord> holdRecords = null;
    private Context context;
    private Runnable getHoldsRunnable = null;
    private Button homeButton;
    private Button myAccountButton;
    private TextView headerTitle;
    private TextView holdsNoText;
    private ProgressBarSupport progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }
        SearchFormat.init(this);

        setContentView(R.layout.holds_list);
        ActionBarUtils.initActionBarForActivity(this);

        holdsNoText = (TextView) findViewById(R.id.holds_number);
        lv = (ListView) findViewById(R.id.holds_item_list);
        context = this;
        accountAccess = AccountAccess.getInstance();
        progress = new ProgressBarSupport();

        holdRecords = new ArrayList<HoldRecord>();
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

        Thread getHoldsThread = new Thread(getHoldsRunnable);
        getHoldsThread.start();

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
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
        progress.dismiss();
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
            // thread to retrieve holds
            Thread getHoldsThread = new Thread(getHoldsRunnable);
            getHoldsThread.start();
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
