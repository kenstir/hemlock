/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
 * Copyright (c) 2025 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */
package net.kenstir.ui.view.search

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.kenstir.hemlock.R
import net.kenstir.ui.App
import net.kenstir.ui.Key
import net.kenstir.ui.util.ActionBarUtils
import net.kenstir.util.Analytics.initialize
import java.util.Locale
import java.util.StringTokenizer

class AdvancedSearchActivity: AppCompatActivity() {
    private var searchTerms: ArrayList<String?>? = null
    private var searchTermTypes: ArrayList<String?>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(this)
        if (!App.isStarted()) {
            App.restartApp(this)
            return
        }

        setContentView(R.layout.advanced_search)
        ActionBarUtils.initActionBarForActivity(this)

        searchTerms = ArrayList()
        searchTermTypes = ArrayList()

        val layout = findViewById<LinearLayout>(R.id.advanced_search_filters)
        val addFilter = findViewById<Button>(R.id.advanced_search_add_filter_button)
        val search_class_spinner = findViewById<Spinner>(R.id.advanced_search_class_spinner)
        val search_contains_spinner = findViewById<Spinner>(R.id.advanced_search_contains_spinner)
        val search_filter_text = findViewById<EditText>(R.id.advanced_search_text)

        addFilter.setOnClickListener {
            val contains_pos = search_contains_spinner.selectedItemPosition
            var query = ""
            val qtype = search_class_spinner.selectedItem.toString().lowercase(Locale.getDefault())
            searchTermTypes!!.add(qtype)
            val filter = search_filter_text.text.toString()

            when (contains_pos) {
                0 -> {
                    // contains
                    query = "$qtype: $filter"
                }
                1 -> {
                    // excludes
                    query = "$qtype:"
                    val str = StringTokenizer(filter)
                    while (str.hasMoreTokens()) {
                        val token = str.nextToken(" ")
                        query = "$query -$token"
                    }
                }
                2 -> {
                    // matches exactly
                    query = "$qtype: \"$filter\""
                }
            }
            searchTerms!!.add(query)
            val text = TextView(this@AdvancedSearchActivity)
            text.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            text.text = query
            layout.addView(text)
        }

        val cancel = findViewById<Button>(R.id.advanced_search_cancel)
        cancel.setOnClickListener {
            //Analytics.logEvent("advsearch_cancel");
            finish()
        }

        val search = findViewById<Button>(R.id.advanced_search_button)
        search.setOnClickListener {
            val returnIntent = Intent()
            returnIntent.putExtra(Key.SEARCH_TEXT, TextUtils.join(" ", searchTerms!!))
            setResult(SearchActivity.RESULT_CODE_SEARCH_BY_KEYWORD, returnIntent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
