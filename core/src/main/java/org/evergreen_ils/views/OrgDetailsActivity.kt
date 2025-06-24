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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.data.OSRFUtils
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.data.model.Organization
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.model.EvergreenOrganization
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.JsonUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.OrgArrayAdapter
import net.kenstir.hemlock.android.ui.ProgressDialogSupport
import net.kenstir.hemlock.android.ui.showAlert
import org.opensrf.util.OSRFObject
import java.util.Date

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

        orgID = if (intent.hasExtra("orgID")) {
            intent.getIntExtra("orgID", 1)
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

    private fun hoursOfOperation(obj: OSRFObject?, day: Int): String? {
        val openTimeApi = obj?.getString("dow_${day}_open")
        val closeTimeApi = obj?.getString("dow_${day}_close")
        val openTime = OSRFUtils.parseHours(openTimeApi)
        val closeTime = OSRFUtils.parseHours(closeTimeApi)
        if (openTime == null || closeTime == null) {
            return null
        }
        if (openTimeApi == closeTimeApi) {
            return "closed"
        }
        val openTimeLocal = OSRFUtils.formatHoursForOutput(openTime)
        val closeTimeLocal = OSRFUtils.formatHoursForOutput(closeTime)
        return "$openTimeLocal - $closeTimeLocal"
    }

    // TODO: [Add Hours of Operation Note field](https://evergreen-ils.org/documentation/release/RELEASE_NOTES_3_10.html#_hours_of_operation_note_field)
    // Look for fields e.g. "dow_0_note"
    private fun loadHours(obj: OSRFObject?) {
        day0Hours?.text = hoursOfOperation(obj, 0)
        day1Hours?.text = hoursOfOperation(obj, 1)
        day2Hours?.text = hoursOfOperation(obj, 2)
        day3Hours?.text = hoursOfOperation(obj, 3)
        day4Hours?.text = hoursOfOperation(obj, 4)
        day5Hours?.text = hoursOfOperation(obj, 5)
        day6Hours?.text = hoursOfOperation(obj, 6)
    }

    private fun onHoursResult(result: Result<OSRFObject?>) {
        when (result) {
            is Result.Success -> loadHours(result.data)
            is Result.Error -> showAlert(result.exception)
        }
    }

    private fun onOrgClosuresResult(result: Result<List<OSRFObject>>) {
        when (result) {
            is Result.Success -> loadClosures(result.data)
            is Result.Error -> showAlert(result.exception)
        }
    }

    private fun loadClosures(closures: List<OSRFObject>) {
        val now = Date()
        val upcomingClosures = mutableListOf<OSRFObject>()
        for (it in closures) {
            val end = it.getDate("close_end")
            if (end != null && end > now && upcomingClosures.size < resources.getInteger(R.integer.ou_upcoming_closures_limit)) {
                upcomingClosures.add(it)
            }
        }
        val rowCount = closuresTable.childCount
        if (rowCount > 2) {
            closuresTable.removeViews(2, rowCount - 2)
        }
        if (upcomingClosures.isEmpty()) {
            findViewById<TableRow>(R.id.org_details_closures_header_row).visibility = View.GONE
            findViewById<TableRow>(R.id.org_details_closures_none_row).visibility = View.VISIBLE
        } else {
            findViewById<TableRow>(R.id.org_details_closures_header_row).visibility = View.VISIBLE
            findViewById<TableRow>(R.id.org_details_closures_none_row).visibility = View.GONE
            addClosureRows(upcomingClosures)
        }
    }

    /** return Triple<dateString, reasonString, isDateRange> */
    private fun getClosureInfo(closure: OSRFObject): Triple<String?, String?, Boolean> {
        val nullReturn = Triple(null, null, false)
        val start = closure.getDate("close_start") ?: return nullReturn
        val end = closure.getDate("close_end") ?: return nullReturn
        val reason = closure.getString("reason") ?: return nullReturn

        val startDateString = OSRFUtils.formatDateForOutput(start)
        val isFullDay = closure.getBoolean("full_day")
        val isMultiDay = closure.getBoolean("multi_day")
        var isDateRange = false
        val dateString = when {
            isMultiDay -> {
                isDateRange = true
                val endDateString = OSRFUtils.formatDateForOutput(end)
                "$startDateString - $endDateString"
            }
            isFullDay -> {
                startDateString
            }
            else -> {
                isDateRange = true
                val startDateTimeString = OSRFUtils.formatDateTimeForOutput(start)
                val endDateTimeString = OSRFUtils.formatDateTimeForOutput(end)
                "$startDateTimeString - $endDateTimeString"
            }
        }
        return Triple(dateString, reason, isDateRange)
    }

    private fun addClosureRows(closures: List<OSRFObject>) {
        // First, walk through the closures to see if any have date ranges.
        // If they do, we need to alter the layout params to make the columns look right.
        val anyClosuresWithDateRange = closures.any {
            val (_, _, isDateRange) = getClosureInfo(it)
            isDateRange
        }
        val dateColumnWidth = if (anyClosuresWithDateRange) 53F else 28F
        val reasonColumnWidth = if (anyClosuresWithDateRange) 47F else 72F

        // Now, add closure rows and maybe tweak the layout
        for (closure in closures) {
            Log.d(TAG, JsonUtils.toJSONString(closure))
            val (dateString, reason, _) = getClosureInfo(closure)

            // create a row, inflate its contents, and add it to the table
            val row = TableRow(baseContext)
            layoutInflater.inflate(R.layout.org_details_closure_item, row)
            closuresTable.addView(row)

            // fill out the details
            val dateTextView = row.findViewById<TextView>(R.id.org_details_closure_item_date)
            dateTextView.text = dateString
            val reasonTextView = row.findViewById<TextView>(R.id.org_details_closure_item_reason)
            reasonTextView.text = reason

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

    private fun loadAddress(obj: OSRFObject?) {
        TODO("fixme")
        //org?.addressObj = obj
        address?.text = org?.getAddress("\n")
        enableButtonsWhenReady()
    }

    private fun onAddressResult(result: Result<OSRFObject?>) {
        when (result) {
            is Result.Success -> loadAddress(result.data)
            is Result.Error -> showAlert(result.exception)
        }
    }

    private fun onOrgLoaded() {
        email?.text = org?.email
        phone?.text = org?.phone
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
                val org = org ?: return@async
                val orgID = orgID ?: return@async

                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Deferred<Any>>()
                progress?.show(this@OrgDetailsActivity, getString(R.string.msg_loading_details))

                Log.d(TAG, "[kcxxx] fetchData ... orgID=$orgID")

                jobs.add(scope.async {
                    val result = App.getServiceConfig().loaderService.loadOrgSettings(orgID)
                    if (result is Result.Error) {
                        throw result.exception
                    }
                })
                jobs.add(scope.async {
                    val result = App.getServiceConfig().loaderService.loadOrgDetails(orgID)
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
