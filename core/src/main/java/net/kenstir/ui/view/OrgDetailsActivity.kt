/*
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

package net.kenstir.ui.view

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.util.component1
import androidx.core.util.component2
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.OrgClosure
import net.kenstir.data.model.OrgHours
import net.kenstir.data.model.Organization
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.Appx
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.Key
import net.kenstir.ui.util.OrgArrayAdapter
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.launchURL
import net.kenstir.ui.util.showAlert
import org.evergreen_ils.system.EgOrg

class OrgDetailsActivity : BaseActivity() {

    private var orgSpinner: Spinner? = null
    private var day0Hours: TextView? = null
    private var day1Hours: TextView? = null
    private var day2Hours: TextView? = null
    private var day3Hours: TextView? = null
    private var day4Hours: TextView? = null
    private var day5Hours: TextView? = null
    private var day6Hours: TextView? = null
    private var day0Note: TextView? = null
    private var day1Note: TextView? = null
    private var day2Note: TextView? = null
    private var day3Note: TextView? = null
    private var day4Note: TextView? = null
    private var day5Note: TextView? = null
    private var day6Note: TextView? = null
    private lateinit var closuresTable: TableLayout
    private var webSite: Button? = null
    private var email: Button? = null
    private var phone: Button? = null
    private var map: Button? = null
    private var address: TextView? = null

    private var orgID: Int? = null
    private var org: Organization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_org_details)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        orgID = if (intent.hasExtra(Key.ORG_ID)) {
            intent.getIntExtra(Key.ORG_ID, 1)
        } else {
            App.account.homeOrg
        }
        org = EgOrg.findOrg(orgID)

        orgSpinner = findViewById(R.id.org_details_spinner)
        day0Hours = findViewById(R.id.org_details_day0hours)
        day1Hours = findViewById(R.id.org_details_day1hours)
        day2Hours = findViewById(R.id.org_details_day2hours)
        day3Hours = findViewById(R.id.org_details_day3hours)
        day4Hours = findViewById(R.id.org_details_day4hours)
        day5Hours = findViewById(R.id.org_details_day5hours)
        day6Hours = findViewById(R.id.org_details_day6hours)
        day0Note = findViewById(R.id.org_details_day0note)
        day1Note = findViewById(R.id.org_details_day1note)
        day2Note = findViewById(R.id.org_details_day2note)
        day3Note = findViewById(R.id.org_details_day3note)
        day4Note = findViewById(R.id.org_details_day4note)
        day5Note = findViewById(R.id.org_details_day5note)
        day6Note = findViewById(R.id.org_details_day6note)
        closuresTable = findViewById(R.id.org_details_closures_table)
        webSite = findViewById(R.id.org_details_web_site)
        email = findViewById(R.id.org_details_email)
        phone = findViewById(R.id.org_details_phone)
        map = findViewById(R.id.org_details_map)
        address = findViewById(R.id.org_details_address)

        val hoursHeader: View? = findViewById(R.id.org_details_opening_hours_header)
        val hoursTable: View? = findViewById(R.id.org_details_opening_hours_table)

        initOrgSpinner()
        initHoursViews(hoursHeader, hoursTable)
        initButtons()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        // It seems onItemSelected is always triggered, so avoid calling fetchData twice on load
        //fetchData()
    }

    private fun initOrgSpinner() {
        val (spinnerLabels, index) = EgOrg.orgSpinnerLabelsAndSelectedIndex(orgID)
        val adapter: ArrayAdapter<String> = OrgArrayAdapter(this, R.layout.org_item_layout, spinnerLabels, false)
        orgSpinner?.adapter = adapter
        orgSpinner?.setSelection(if (index > 0) index else 0)
        Log.d(TAG, "[fetch] setSelection $index")
        orgSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                org = EgOrg.visibleOrgs[position]
                orgID = org?.id
                Log.d(TAG, "[fetch] onItemSelected $position, orgID=$orgID")
                fetchData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initHoursViews(hoursHeader: View?, hoursTable: View?) {
        if (!resources.getBoolean(R.bool.ou_enable_hours_of_operation)) {
            hoursHeader?.visibility = View.GONE
            hoursTable?.visibility = View.GONE
        }
    }

    private fun initButtons() {
        webSite?.setOnClickListener {
            launchURL(org?.infoURL)
        }
        email?.setOnClickListener {
            sendEmail(org?.email)
        }
        phone?.setOnClickListener {
            dialPhone(org?.phone)
        }
        map?.setOnClickListener {
            launchMap(org?.getAddress(" "))
        }
        enableButtonsWhenReady()
    }

    private fun enableButtonsWhenReady() {
        webSite?.isEnabled = !(org?.infoURL.isNullOrEmpty())
        email?.isEnabled = !(org?.email.isNullOrEmpty())
        phone?.isEnabled = !(org?.phone.isNullOrEmpty())
        map?.isEnabled = org?.hasAddress ?: false
    }

    private fun testNote(note: String?): String? {
        return note
//        val junkNotes = listOf(
//            "Closed for inventory",
//            "Closed for staff training",
//            "Open by appointment only",
//            "Closed due to weather conditions",
//            "Renovations in progress, limited services available"
//        )
//        return if (Random.nextInt(100) < 50) {
//            junkNotes.random()
//        } else {
//            note
//        }
    }

    private fun loadHours(hours: OrgHours) {
        day0Hours?.text = hours.day0Hours
        day1Hours?.text = hours.day1Hours
        day2Hours?.text = hours.day2Hours
        day3Hours?.text = hours.day3Hours
        day4Hours?.text = hours.day4Hours
        day5Hours?.text = hours.day5Hours
        day6Hours?.text = hours.day6Hours

        day0Note?.text = testNote(hours.day0Note)
        day1Note?.text = testNote(hours.day1Note)
        day2Note?.text = testNote(hours.day2Note)
        day3Note?.text = testNote(hours.day3Note)
        day4Note?.text = testNote(hours.day4Note)
        day5Note?.text = testNote(hours.day5Note)
        day6Note?.text = testNote(hours.day6Note)
    }

    private fun loadClosures(closures: List<OrgClosure>) {
        val rowCount = closuresTable.childCount
        if (rowCount > 2) {
            closuresTable.removeViews(2, rowCount - 2)
        }
        if (closures.isEmpty()) {
            findViewById<TableRow>(R.id.org_details_closures_header_row).visibility = View.GONE
            findViewById<TableRow>(R.id.org_details_closures_none_row).visibility = View.VISIBLE
        } else {
            findViewById<TableRow>(R.id.org_details_closures_header_row).visibility = View.VISIBLE
            findViewById<TableRow>(R.id.org_details_closures_none_row).visibility = View.GONE
            addClosureRows(closures.take(resources.getInteger(R.integer.ou_upcoming_closures_limit)))
        }
    }

    private fun addClosureRows(closures: List<OrgClosure>) {
        for (closure in closures) {
            val info = closure.toInfo()

            // inflate the layout into a new TableRow
            val row = TableRow(this)
            layoutInflater.inflate(R.layout.org_details_closure_item, row, true)

            // fill out the details
            val dateTextView = row.findViewById<TextView>(R.id.org_details_closure_item_date)
            dateTextView.text = info.dateString
            val reasonTextView = row.findViewById<TextView>(R.id.org_details_closure_item_reason)
            reasonTextView.text = info.reason

            closuresTable.addView(row)
        }
    }

    private fun onOrgLoaded() {
        email?.text = org?.email
        phone?.text = org?.phone
        address?.text = org?.getAddress("\n")
        org?.hours?.let { loadHours(it) }
        loadClosures(org?.closures ?: emptyList())
        enableButtonsWhenReady()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchData() {
        scope.async {
            try {
                val account = App.account
                val orgID = orgID ?: return@async

                val start = System.currentTimeMillis()
                showBusy(R.string.msg_loading_details)

                Log.d(TAG, "[fetch] fetchData ... orgID=$orgID")

                val jobs = mutableListOf<Deferred<Any>>()
                jobs.add(scope.async {
                    val result = Appx.svc.orgService.loadOrgSettings(orgID)
                    if (result is Result.Error) {
                        throw result.exception
                    }
                })
                jobs.add(scope.async {
                    val result = Appx.svc.orgService.loadOrgDetails(account, orgID)
                    if (result is Result.Error) {
                        throw result.exception
                    }
                })

                // await all deferred (see awaitAll doc for differences)
                jobs.map { it.await() }
                onOrgLoaded()
                Log.logElapsedTime(TAG, start, "[fetch] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                hideBusy()
            }
        }
    }

    companion object {
        private const val TAG = "OrgDetailsActivity"
    }
}
