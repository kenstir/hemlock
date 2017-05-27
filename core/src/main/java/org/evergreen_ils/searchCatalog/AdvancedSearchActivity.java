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

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import org.evergreen_ils.R;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class AdvancedSearchActivity extends AppCompatActivity {

    private final static String TAG = AdvancedSearchActivity.class.getSimpleName();

    private ArrayList<String> searchTerms;
    private String advancedSearchFormattedText;

    public static final int RESULT_ADVANCED_SEARCH = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.advanced_search);
        ActionBarUtils.initActionBarForActivity(this);

        searchTerms = new ArrayList<String>();
        advancedSearchFormattedText = new String();

        final LinearLayout layout = (LinearLayout) findViewById(R.id.advanced_search_filters);
        Button addFilter = (Button) findViewById(R.id.advanced_search_add_filter_button);
        final Spinner search_qtype_spinner = (Spinner) findViewById(R.id.advanced_search_qtype_spinner);
        final Spinner search_contains_spinner = (Spinner) findViewById(R.id.advanced_search_contains_spinner);
        final EditText search_filter_text = (EditText) findViewById(R.id.advanced_search_text);

        addFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                int contains_pos = search_contains_spinner.getSelectedItemPosition();
                String query = "";
                String qtype = search_qtype_spinner.getSelectedItem().toString().toLowerCase();
                String filter = search_filter_text.getText().toString();

                switch (contains_pos) {
                case 0:
                    // contains
                    query = qtype + ": " + filter;
                    break;
                case 1:
                    // excludes
                    query = qtype + ":";
                    StringTokenizer str = new StringTokenizer(filter);
                    while (str.hasMoreTokens()) {
                        String token = str.nextToken(" ");
                        query = query + " -" + token;
                    }
                    break;
                case 2:
                    // matches exactly
                    query = qtype + " \"" + filter + "\"";
                    break;
                }
                searchTerms.add(query);
                TextView text = new TextView(AdvancedSearchActivity.this);
                text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                text.setText(query);
                layout.addView(text);
            }
        });

        Button cancel = (Button) findViewById(R.id.advanced_search_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button search = (Button) findViewById(R.id.advanced_search_button);
        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("advancedSearchText", TextUtils.join(" ", searchTerms));
                setResult(RESULT_ADVANCED_SEARCH, returnIntent);
                finish();
            }
        });

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

}
