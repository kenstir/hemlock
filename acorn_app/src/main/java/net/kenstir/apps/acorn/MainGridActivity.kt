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

package net.kenstir.apps.acorn

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.apps.bibliomation.R
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.GridButton
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.views.BarcodeActivity
import org.evergreen_ils.views.CheckoutsActivity
import org.evergreen_ils.views.FinesActivity
import org.evergreen_ils.views.MainActivity
import org.evergreen_ils.views.OrgDetailsActivity
import org.evergreen_ils.views.bookbags.BookBagsActivity
import org.evergreen_ils.views.holds.HoldsActivity
import org.evergreen_ils.views.search.SearchActivity

class MainGridActivity : BaseActivity() {
    private val TAG = javaClass.simpleName
    private val SPAN_COUNT = 2

    private var rv: RecyclerView? = null
    private var adapter: GridButtonViewAdapter? = null
    private var items = ArrayList<GridButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // not super
        if (isRestarting) return

        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        setContentView(R.layout.activity_main_grid)

        rv = findViewById(R.id.recycler_view)
        setupRecyclerView()
        addGridButtons()
    }

    private fun setupRecyclerView() {
        rv?.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        adapter = GridButtonViewAdapter(items)
        rv?.adapter = adapter
    }

    private fun getDrawable(drawableId: Int, colorId: Int): Drawable {
        val drawable = resources.getDrawable(drawableId, null)
//        drawable.setTint(ContextCompat.getColor(this, colorId))
        return drawable
    }

    private fun addGridButtons() {
        val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
//        val defaultUrl = "https://google.com"
        val defaultUrl: String? = null

        // Show Card
        val cardDrawable = getDrawable(R.drawable.acorn_id_card_light, R.color.cwmars_violet)
        items.add(GridButton("Digital Library Card",
            cardDrawable,
            "library card") {
            startActivity(Intent(this, BarcodeActivity::class.java))
        })

        // Search
        val searchDrawable = getDrawable(R.drawable.acorn_magnifying_glass_light, R.color.cwmars_red)
        items.add(GridButton("Search Catalog",
            searchDrawable,
            "magnifying glass") {
            startActivity(Intent(this, SearchActivity::class.java))
        })

        // Org Details
        items.add(GridButton("Library Hours & Info",
            resources.getDrawable(R.drawable.acorn_square_info_light, null),
            "info") {
            startActivity(Intent(this, OrgDetailsActivity::class.java))
        })

//        // Events
//        val eventsUrl = homeOrg?.eventsURL ?: defaultUrl
//        if (!eventsUrl.isNullOrEmpty()) {
//            items.add(GridButton("Events",
//                resources.getDrawable(R.drawable.acorn_calendar_day_light, null),
//                "calendar") {
//                launchURL(eventsUrl)
//            })
//        }
//
//        // E-resources
//        val eresourcesUrl = homeOrg?.eresourcesUrl ?: defaultUrl
//        if (!eresourcesUrl.isNullOrEmpty()) {
//            items.add(GridButton("Ebooks & Digital",
//                resources.getDrawable(R.drawable.acorn_light_book_circle_arrow_down, null),
//                "e-book") {
//                launchURL(eresourcesUrl)
//            })
//        }
//
//        // Museum Passes
//        val passesUrl = homeOrg?.museumPassesUrl ?: defaultUrl
//        if (!passesUrl.isNullOrEmpty()) {
//            items.add(GridButton("Museum Passes",
//                resources.getDrawable(R.drawable.acorn_ticket_light, null),
//                "ticket") {
//                launchURL(passesUrl)
//            })
//        }
//
//        // Meeting Rooms
//        val roomsUrl = homeOrg?.meetingRoomsUrl ?: defaultUrl
//        if (!roomsUrl.isNullOrEmpty()) {
//            items.add(GridButton("Meeting Rooms",
//                resources.getDrawable(R.drawable.acorn_users_light, null),
//                "group of people") {
//                launchURL(roomsUrl)
//            })
//        }

        // Items Checked Out
        items.add(GridButton("Items Checked Out",
            resources.getDrawable(R.drawable.ic_checkouts_48, null),
            "items checked out") {
            startActivity(Intent(this, BarcodeActivity::class.java))//todo
        })

        // Holds
        items.add(GridButton("Holds",
            resources.getDrawable(R.drawable.ic_holds_48, null),
            "items on hold") {
            startActivity(Intent(this, BarcodeActivity::class.java))//todo
        })

        // Fines
        items.add(GridButton("Fines",
            resources.getDrawable(R.drawable.ic_fines_48, null),
            "fines") {
            startActivity(Intent(this, BarcodeActivity::class.java))//todo
        })

        // My Lists
        items.add(GridButton("My Lists",
            resources.getDrawable(R.drawable.ic_lists_48, null),
            "my lists") {
            startActivity(Intent(this, BarcodeActivity::class.java))//todo
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (menuItemHandler?.onItemSelected(this, id, "main_option_menu") == true)
            return true
        return if (handleMenuAction(id)) true else super.onOptionsItemSelected(item)
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
        } else if (id == org.evergreen_ils.R.id.main_events_button) {
            launchURL(getEventsUrl())
        } else if (id == org.evergreen_ils.R.id.main_showcard_button) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (menuItemHandler != null) {
            menuItemHandler?.onItemSelected(this, id, "main_button")
        }
    }
}
