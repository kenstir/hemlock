/*
 * Copyright (c) 2023 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.views

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.evergreen_ils.BuildConfig
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.GridButton
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.ui.MainBaseActivity
import org.evergreen_ils.views.bookbags.BookBagsActivity
import org.evergreen_ils.views.holds.HoldsActivity
import org.evergreen_ils.views.search.SearchActivity

class MainGridActivity : MainBaseActivity() {
    private val TAG = javaClass.simpleName
    private val SPAN_COUNT = 2

    private var rv: RecyclerView? = null
    private var adapter: GridButtonViewAdapter? = null
    private var items = ArrayList<GridButton>()

    // arrange for all buttons to be visible when debugging
    private val showButtonsWithoutURLsForDebugging = BuildConfig.DEBUG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        setContentView(R.layout.activity_main_grid)

        rv = findViewById(R.id.recycler_view)
        setupRecyclerView()
        addGridButtons()
        setupBottomRowButtons()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object {}.javaClass.enclosingMethod?.name ?: "")
        fetchData()
    }

    private fun fetchData() {
        initializePushNotifications()
//        loadUnreadMessageCount()
    }

    private fun setupRecyclerView() {
        rv?.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        adapter = GridButtonViewAdapter(items)
        rv?.adapter = adapter
    }

    private fun addGridButtons() {
        // Show Card
        items.add(GridButton(resources.getString(R.string.barcode_title),
            resources.getDrawable(R.drawable.ic_grid_show_card_48, null),
            null) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        })

        // Search
        items.add(GridButton(resources.getString(R.string.title_search),
            resources.getDrawable(R.drawable.ic_grid_search_48, null),
            null) {
            startActivity(Intent(this, SearchActivity::class.java))
        })

        // Org Details
        items.add(GridButton(resources.getString(R.string.title_library_info),
            resources.getDrawable(R.drawable.ic_grid_library_info_48, null),
            null) {
            startActivity(Intent(this, OrgDetailsActivity::class.java))
        })

        // Items Checked Out
        items.add(GridButton(resources.getString(R.string.title_check_out),
            resources.getDrawable(R.drawable.ic_grid_checkouts_48, null),
            null) {
            startActivity(Intent(this, CheckoutsActivity::class.java))
        })

        // Fines
        items.add(GridButton(resources.getString(R.string.title_fines),
            resources.getDrawable(R.drawable.ic_grid_fines_48, null),
            null) {
            startActivity(Intent(this, FinesActivity::class.java))
        })

        // Holds
        items.add(GridButton(resources.getString(R.string.title_holds),
            resources.getDrawable(R.drawable.ic_grid_holds_48, null),
            null) {
            startActivity(Intent(this, HoldsActivity::class.java))
        })

        // My Lists
        items.add(GridButton(resources.getString(R.string.title_my_lists),
            resources.getDrawable(R.drawable.ic_grid_bookbags_48, null),
            null) {
            startActivity(Intent(this, BookBagsActivity::class.java))
        })

        // Events
        val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
        val eventsUrl = homeOrg?.eventsURL
        if (!eventsUrl.isNullOrEmpty() || showButtonsWithoutURLsForDebugging) {
            items.add(GridButton(resources.getString(R.string.title_events),
                resources.getDrawable(R.drawable.ic_grid_events_48, null),
                null) {
                launchURL(eventsUrl)
            })
        }
    }

    fun setupBottomRowButtons() {
        val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
        var numVisible = 0

        // E-books
        val ebooksUrl = homeOrg?.eresourcesUrl
        if (!ebooksUrl.isNullOrEmpty() || showButtonsWithoutURLsForDebugging) {
            ++numVisible
        } else {
            findViewById<Button>(R.id.grid_ebooks_button)?.visibility = View.GONE
        }

        // Meeting Rooms
        val meetingRoomsUrl = homeOrg?.meetingRoomsUrl
        if (!meetingRoomsUrl.isNullOrEmpty() || showButtonsWithoutURLsForDebugging) {
            ++numVisible
        } else {
            findViewById<Button>(R.id.grid_meeting_rooms_button)?.visibility = View.GONE
        }

        // Museum Passes
        val museumPassesUrl = homeOrg?.museumPassesUrl
        if (!museumPassesUrl.isNullOrEmpty() || showButtonsWithoutURLsForDebugging) {
            ++numVisible
        } else {
            findViewById<Button>(R.id.grid_museum_passes_button)?.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(org.evergreen_ils.R.menu.menu_main, menu)

        // remove items we don't need
        if (TextUtils.isEmpty(feedbackUrl))
            menu.removeItem(org.evergreen_ils.R.id.action_feedback)

        // set up the messages action view, it didn't work when set in xml
        if (!resources.getBoolean(org.evergreen_ils.R.bool.ou_enable_messages)) {
            menu.removeItem(org.evergreen_ils.R.id.action_messages)
        }

        return true
    }

    fun onButtonClick(v: View) {
        //throw RuntimeException("Test Crash") // test Crashlytics
        val id = v.id
        if (id == org.evergreen_ils.R.id.main_checkouts_button) {
            startActivity(Intent(this, CheckoutsActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_holds_button) {
            startActivity(Intent(this, HoldsActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_fines_button) {
            startActivity(Intent(this, FinesActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_my_lists_button) {
            startActivity(Intent(this, BookBagsActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_search_button) {
            startActivity(Intent(this, SearchActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_library_info_button) {
            startActivity(Intent(this, OrgDetailsActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_showcard_button) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (id == org.evergreen_ils.R.id.main_events_button) {
            launchURL(getEventsUrl())
        } else if (id == R.id.grid_ebooks_button) {
            launchURL(getEbooksUrl())
        } else if (id == R.id.grid_meeting_rooms_button) {
            launchURL(getMeetingRoomsUrl())
        } else if (id == R.id.grid_museum_passes_button) {
            launchURL(getMuseumPassesUrl())
        } else if (menuItemHandler != null) {
            menuItemHandler?.onItemSelected(this, id, "main_button")
        }
    }
}
