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

import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.opensrf.util.GatewayResponse;

public class CopyInformationActivity extends ActionBarActivity {

    private static final String TAG = CopyInformationActivity.class.getSimpleName();
    private Context context;
    private RecordInfo record;
    private Integer orgID;
    private EvergreenServer eg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.copy_information_more);
        ActionBarUtils.initActionBarForActivity(this);

        eg = EvergreenServer.getInstance();
        context = this;
        record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
        orgID = getIntent().getIntExtra("orgID", 1);

        initCopyInfo();
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
        LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout insertPoint = (LinearLayout) findViewById(R.id.record_details_copy_information);
        if (record.copyInformationList == null)
            return;
        for (int i = 0; i < record.copyInformationList.size(); i++) {
            View copy_info_view = inf.inflate(R.layout.copy_information, null);

            // fill in any details dynamically here
            TextView library = (TextView) copy_info_view.findViewById(R.id.copy_information_library);
            TextView call_number = (TextView) copy_info_view.findViewById(R.id.copy_information_call_number);
            TextView copy_location = (TextView) copy_info_view.findViewById(R.id.copy_information_copy_location);

            library.setText(eg.getOrganizationName(record.copyInformationList.get(i).org_id));
            call_number.setText(record.copyInformationList.get(i).getCallNumber());
            copy_location.setText(record.copyInformationList.get(i).copy_location);

            // insert into main view
            insertPoint.addView(copy_info_view, new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));

            LinearLayout copy_statuses = (LinearLayout) copy_info_view.findViewById(R.id.copy_information_statuses);

            CopyInformation info = record.copyInformationList.get(i);
            Set<Entry<String, String>> set = info.statusInformation.entrySet();
            Iterator<Entry<String, String>> it = set.iterator();
            while (it.hasNext()) {
                Entry<String, String> ent = it.next();
                TextView statusName = new TextView(context);
                statusName.setText(ent.getKey() + ": " + ent.getValue());
                copy_statuses.addView(statusName, new LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        }
    }

    private void initCopyInfo() {
        Log.d(TAG, "kcx.initCopyInfo, id="+record.doc_id+" info="+record.copyCountInformationList);
        SearchCatalog search = SearchCatalog.getInstance();
        if (record.copyInformationList == null) {
            final long start_ms = System.currentTimeMillis();
            Organization org = eg.getOrganization(orgID);
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
                            SearchCatalog.setCopyLocationCounts(record, response);
                            updateCopyInfo();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "kcx.initCopyInfo caught", error);
                            SearchCatalog.setCopyLocationCounts(record, null);
                        }
                    });
            VolleyWrangler.getInstance(this).addToRequestQueue(r);
        }
    }
}
