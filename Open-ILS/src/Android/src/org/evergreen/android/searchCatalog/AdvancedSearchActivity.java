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
package org.evergreen.android.searchCatalog;

import java.util.StringTokenizer;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.views.AccountScreenDashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class AdvancedSearchActivity extends Activity {

    private String TAG = "AdvancedSearchActivity";

    private AccountAccess accountAccess = null;

    private Context context;

    private Button homeButton;

    private Button myAccountButton;

    private TextView headerTitle;

    private StringBuilder advancedSearchFormattedText;

    public static final int RESULT_ADVANCED_SEARCH = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.advanced_search);

        // header portion actions
        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.advanced_search);

        advancedSearchFormattedText = new StringBuilder();

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

        context = this;

        final LinearLayout layout = (LinearLayout) findViewById(R.id.advanced_search_filters);

        Button addFilter = (Button) findViewById(R.id.advanced_search_add_filter_button);

        final Spinner search_index = (Spinner) findViewById(R.id.advanced_spinner_index);
        final Spinner search_option = (Spinner) findViewById(R.id.advanced_spinner_option);
        final EditText search_filter_text = (EditText) findViewById(R.id.advanced_search_text);

        addFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                int searchOptionVal = search_option.getSelectedItemPosition();

                String searchText = search_index.getSelectedItem().toString()
                        .toLowerCase()
                        + ": ";

                advancedSearchFormattedText.append(search_index
                        .getSelectedItem().toString().toLowerCase()
                        + ": ");

                switch (searchOptionVal) {

                case 0: {
                    // contains
                    advancedSearchFormattedText.append(search_filter_text
                            .getText().toString());
                    searchText = searchText
                            + search_filter_text.getText().toString();
                }
                    break;
                case 1: {
                    // excludes

                    StringTokenizer str = new StringTokenizer(
                            search_filter_text.getText().toString());

                    while (str.hasMoreTokens()) {
                        String token = str.nextToken(" ");
                        advancedSearchFormattedText.append(" -" + token);
                        searchText = searchText + " -" + token;
                    }

                }
                    break;
                case 2: {
                    // matches exactly
                    advancedSearchFormattedText.append(" \""
                            + search_filter_text.getText().toString() + "\"");
                    searchText = searchText + " \""
                            + search_filter_text.getText().toString() + "\"";
                }
                    break;

                }

                TextView text = new TextView(context);
                text.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                        LayoutParams.WRAP_CONTENT));
                text.setText(searchText);
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
                returnIntent.putExtra("advancedSearchText",
                        advancedSearchFormattedText.toString());
                setResult(RESULT_ADVANCED_SEARCH, returnIntent);
                finish();
            }
        });

    }

}
