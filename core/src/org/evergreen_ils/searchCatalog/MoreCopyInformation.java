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
import org.evergreen_ils.R;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MoreCopyInformation extends ActionBarActivity {

    private Context context;

    private RecordInfo record;

    private GlobalConfigs globalConfigs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.copy_information_more);
        ActionBarUtils.initActionBarForActivity(this);

        globalConfigs = GlobalConfigs.getInstance(context);
        context = this;
        record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");

        LayoutInflater inf = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // insert into main view
        LinearLayout insertPoint = (LinearLayout) findViewById(R.id.record_details_copy_information);
        addCopyInfo(inf, insertPoint);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // make the action bar "up" caret work like "back"
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addCopyInfo(LayoutInflater inflater, LinearLayout insertPoint) {

        for (int i = 0; i < record.copyInformationList.size(); i++) {

            View copy_info_view = inflater.inflate(R.layout.copy_information, null);

            // fill in any details dynamically here
            TextView library = (TextView) copy_info_view.findViewById(R.id.copy_information_library);
            TextView call_number = (TextView) copy_info_view.findViewById(R.id.copy_information_call_number);
            TextView copy_location = (TextView) copy_info_view.findViewById(R.id.copy_information_copy_location);

            library.setText(globalConfigs.getOrganizationName(record.copyInformationList.get(i).org_id));
            call_number.setText(record.copyInformationList.get(i).call_number_sufix);
            copy_location.setText(record.copyInformationList.get(i).copy_location);

            // insert into main view
            insertPoint.addView(copy_info_view, new ViewGroup.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

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
}
