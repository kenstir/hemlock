/*
 * Copyright (c) 2020 Kenneth H. Cox
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.views

import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.util.component1
import androidx.core.util.component2
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Key
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.data.model.Organization
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.OrgArrayAdapter
import net.kenstir.hemlock.android.ui.ProgressDialogSupport
import net.kenstir.hemlock.android.ui.showAlert
import net.kenstir.hemlock.data.model.OrgClosure
import net.kenstir.hemlock.data.model.OrgHours

class OrgDetailsActivity : BaseActivity() {
    private val TAG = OrgDetailsActivity::class.java.simpleName

    private var orgSpinner: Spinner? = null
    private var day0Hours: TextView? = null
    private var day1Hours: TextView? = null
    private var day2Hours: TextView? = null
    private var day3Hours: TextView? = null
    private var day4Hours: TextView? = null
    private var day5Hours: TextView? = null
    private var day6Hours: TextView? = null
    private lateinit var closuresTable: TableLayout
    private var webSite: Button? = null
    private var email: Button? = null
    private var phone: Button? = null
    private var map: Button? = null
    private var address: TextView? = null
    private var progress: ProgressDialogSupport? = null

    private var orgID: Int? = null
    private var org: Organization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_org_details)

        orgID = if (intent.hasExtra(Key.ORG_ID)) {
            intent.getIntExtra(Key.ORG_ID, 1)
        } else {
            App.getAccount().homeOrg
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
        closuresTable = findViewById(R.id.org_details_closures_table)
        webSite = findViewById(R.id.org_details_web_site)
        email = findViewById(R.id.org_details_email)
        phone = findViewById(R.id.org_details_phone)
        map = findViewById(R.id.org_details_map)
        address = findViewById(R.id.org_details_address)

        val hours_header: View? = findViewById(R.id.org_details_opening_hours_header)
        val hours_table: View? = findViewById(R.id.org_details_opening_hours_table)

        progress = ProgressDialogSupport()

        initOrgSpinner()
        initHoursViews(hours_header, hours_table)
        initButtons()
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
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
        Log.d(TAG, "[kcxxx] setSelection $index")
        orgSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                org = EgOrg.visibleOrgs[position]
                orgID = org?.id
                Log.d(TAG, "[kcxxx] onItemSelected $position, orgID=$orgID")
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

    private fun loadHours(hours: OrgHours) {
        day0Hours?.text = hours.day0Hours
        day1Hours?.text = hours.day1Hours
        day2Hours?.text = hours.day2Hours
        day3Hours?.text = hours.day3Hours
        day4Hours?.text = hours.day4Hours
        day5Hours?.text = hours.day5Hours
        day6Hours?.text = hours.day6Hours
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
        // First, walk through the closures to see if any have date ranges.
        // If they do, we need to alter the layout params to make the columns look right.
        val anyClosuresWithDateRange = closures.any {
            it.toInfo().isDateRange
        }
        val dateColumnWidth = if (anyClosuresWithDateRange) 53F else 28F
        val reasonColumnWidth = if (anyClosuresWithDateRange) 47F else 72F

        // Now, add closure rows and maybe tweak the layout
        for (closure in closures) {
            val info = closure.toInfo()

            // create a row, inflate its contents, and add it to the table
            val row = TableRow(baseContext)
            layoutInflater.inflate(R.layout.org_details_closure_item, row)
            closuresTable.addView(row)

            // fill out the details
            val dateTextView = row.findViewById<TextView>(R.id.org_details_closure_item_date)
            dateTextView.text = info.dateString
            val reasonTextView = row.findViewById<TextView>(R.id.org_details_closure_item_reason)
            reasonTextView.text = info.reason

            // tweak layout to give more space to the date column
            if (anyClosuresWithDateRange) {
                val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0F, resources.displayMetrics).toInt()
                val height = ViewGroup.LayoutParams.MATCH_PARENT
                dateTextView.layoutParams = TableRow.LayoutParams(width, height, dateColumnWidth)
                reasonTextView.layoutParams = TableRow.LayoutParams(width, height, reasonColumnWidth)
            }
        }

        // tweak header layout also
        if (anyClosuresWithDateRange) {
            val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0F, resources.displayMetrics).toInt()
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            val dateHeaderView = findViewById<TextView>(R.id.org_details_closures_header_date)
            val reasonHeaderView = findViewById<TextView>(R.id.org_details_closures_header_reason)
            dateHeaderView.layoutParams = TableRow.LayoutParams(width, height, dateColumnWidth)
            reasonHeaderView.layoutParams = TableRow.LayoutParams(width, height, reasonColumnWidth)
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
                val account = App.getAccount()
                val org = org ?: return@async
                val orgID = orgID ?: return@async

                val start = System.currentTimeMillis()
                progress?.show(this@OrgDetailsActivity, getString(R.string.msg_loading_details))

                Log.d(TAG, "[kcxxx] fetchData ... orgID=$orgID")

                val jobs = mutableListOf<Deferred<Any>>()
                jobs.add(scope.async {
                    val result = App.getServiceConfig().orgService.loadOrgSettings(orgID)
                    if (result is Result.Error) {
                        throw result.exception
                    }
                })
                jobs.add(scope.async {
                    val result = App.getServiceConfig().orgService.loadOrgDetails(account, orgID)
                    if (result is Result.Error) {
                        throw result.exception
                    }
                })

                // await all deferred (see awaitAll doc for differences)
                jobs.map { it.await() }
                onOrgLoaded()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }
}
