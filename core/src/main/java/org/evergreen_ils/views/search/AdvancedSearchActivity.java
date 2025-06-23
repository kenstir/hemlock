/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.views.search;

import static org.evergreen_ils.ConstKt.KEY_SEARCH_TEXT;
import static org.evergreen_ils.views.search.SearchActivity.RESULT_CODE_SEARCH_BY_KEYWORD;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.kenstir.hemlock.R;
import net.kenstir.hemlock.android.App;
import net.kenstir.hemlock.android.ui.ActionBarUtils;
import net.kenstir.hemlock.android.Analytics;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class AdvancedSearchActivity extends AppCompatActivity {

    private final static String TAG = AdvancedSearchActivity.class.getSimpleName();

    private ArrayList<String> searchTerms;
    private ArrayList<String> searchTermTypes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.initialize(this);
        if (!App.isStarted()) {
            App.restartApp(this);
            return;
        }

        setContentView(R.layout.advanced_search);
        ActionBarUtils.initActionBarForActivity(this);

        searchTerms = new ArrayList<>();
        searchTermTypes = new ArrayList<>();

        final LinearLayout layout = findViewById(R.id.advanced_search_filters);
        Button addFilter = findViewById(R.id.advanced_search_add_filter_button);
        final Spinner search_class_spinner = findViewById(R.id.advanced_search_class_spinner);
        final Spinner search_contains_spinner = findViewById(R.id.advanced_search_contains_spinner);
        final EditText search_filter_text = findViewById(R.id.advanced_search_text);

        addFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                int contains_pos = search_contains_spinner.getSelectedItemPosition();
                String query = "";
                String qtype = search_class_spinner.getSelectedItem().toString().toLowerCase();
                searchTermTypes.add(qtype);
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

        Button cancel = findViewById(R.id.advanced_search_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                Analytics.logEvent("advsearch_cancel");
                finish();
            }
        });

        Button search = findViewById(R.id.advanced_search_button);
        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String types = TextUtils.join("|", searchTermTypes);
                //Analytics.logEvent("advsearch_search", "search_type", types);//TODO
                Intent returnIntent = new Intent();
                returnIntent.putExtra(KEY_SEARCH_TEXT, TextUtils.join(" ", searchTerms));
                setResult(RESULT_CODE_SEARCH_BY_KEYWORD, returnIntent);
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
