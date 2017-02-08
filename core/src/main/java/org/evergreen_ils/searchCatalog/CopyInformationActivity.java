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
package org.evergreen_ils.searchCatalog;

import java.util.*;
import java.util.Map.Entry;

import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import org.opensrf.util.GatewayResponse;

public class CopyInformationActivity extends ActionBarActivity {

    private static final String TAG = CopyInformationActivity.class.getSimpleName();
    private RecordInfo record;
    private Integer orgID;
    private ListView lv;
    private ArrayList<CopyLocationCounts> copyInfoRecords;
    private CopyInformationArrayAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.copy_information_list);
        ActionBarUtils.initActionBarForActivity(this);

        if (savedInstanceState != null) {
            record = (RecordInfo) savedInstanceState.getSerializable("recordInfo");
            orgID = savedInstanceState.getInt("orgID");
        } else {
            record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
            orgID = getIntent().getIntExtra("orgID", 1);
        }

        lv = (ListView) findViewById(R.id.copy_information_list);
        copyInfoRecords = new ArrayList<>();
        listAdapter = new CopyInformationArrayAdapter(this,
                R.layout.copy_information_item, copyInfoRecords);
        lv.setAdapter(listAdapter);

        TextView summaryText = (TextView) findViewById(R.id.copy_information_summary);
        summaryText.setText(RecordLoader.getCopySummary(record, orgID, this));

        initCopyLocationCounts();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("recordInfo", record);
        outState.putInt("orgID", orgID);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateCopyInfo() {
        if (record.copyLocationCountsList == null)
            return;
        copyInfoRecords.clear();
        for (CopyLocationCounts info : record.copyLocationCountsList) {
            copyInfoRecords.add(info);
        }
        //todo figure out how to display this like GAPINES
        //if (getResources().getBoolean(R.bool.ou_flatten_org_tree))
        final EvergreenServer eg = EvergreenServer.getInstance();
        Collections.sort(copyInfoRecords, new Comparator<CopyLocationCounts>() {
            @Override
            public int compare(CopyLocationCounts a, CopyLocationCounts b) {
                return eg.getOrganizationName(a.org_id).compareTo(eg.getOrganizationName(b.org_id));
            }
        });
        listAdapter.notifyDataSetChanged();
    }

    private void initCopyLocationCounts() {
        if (record.copyLocationCountsList != null) {
            updateCopyInfo();
        } else {
            final long start_ms = System.currentTimeMillis();
            Organization org = EvergreenServer.getInstance().getOrganization(orgID);
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.SEARCH, Api.COPY_LOCATION_COUNTS,
                    new Object[]{record.doc_id, org.id, org.level}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.NORMAL,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            long duration_ms = System.currentTimeMillis() - start_ms;
                            Log.d(TAG, "kcx.fetch "+record.doc_id+" took " + duration_ms + "ms");
                            RecordInfo.setCopyLocationCounts(record, response);
                            updateCopyInfo();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "kcx.initCopyLocationCounts caught", error);
                            RecordInfo.setCopyLocationCounts(record, null);
                        }
                    });
            VolleyWrangler.getInstance(this).addToRequestQueue(r);
        }
    }

    class CopyInformationArrayAdapter extends ArrayAdapter<CopyLocationCounts> {
        private TextView copyLibraryText;
        private TextView copyCallNumberText;
        private TextView copyLocationText;
        private TextView copyStatusesText;
        private List<CopyLocationCounts> records;

        public CopyInformationArrayAdapter(Context context, int textViewResourceId, List<CopyLocationCounts> objects) {
            super(context, textViewResourceId, objects);
            records = objects;
        }

        public int getCount() {
            return this.records.size();
        }

        public CopyLocationCounts getItem(int index) {
            return this.records.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            final CopyLocationCounts item = getItem(position);

            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.copy_information_item, parent, false);
            }

            copyLibraryText = (TextView) row.findViewById(R.id.copy_information_library);
            copyCallNumberText = (TextView) row.findViewById(R.id.copy_information_call_number);
            copyLocationText = (TextView) row.findViewById(R.id.copy_information_copy_location);
            copyStatusesText = (TextView) row.findViewById(R.id.copy_information_statuses);

            copyLibraryText.setText(EvergreenServer.getInstance().getOrganizationName(item.org_id));
            copyCallNumberText.setText(item.getCallNumber());
            copyLocationText.setText(item.copy_location);

            List<String> statuses = item.getCountsByStatus();
            copyStatusesText.setText(TextUtils.join("\n", statuses));

            return row;
        }
    }
}
