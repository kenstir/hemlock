/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
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
package org.evergreen_ils.searchCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import org.evergreen_ils.Api
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.CopyLocationCounts
import org.evergreen_ils.net.Gateway.buildUrl
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.Volley
import org.evergreen_ils.system.EgOrg.findOrg
import org.evergreen_ils.system.EgOrg.getOrgNameSafe
import org.evergreen_ils.utils.ui.ActionBarUtils
import org.evergreen_ils.views.OrgDetailsActivity
import java.util.*

// TODO: derive from BaseActivity()
class CopyInformationActivity : AppCompatActivity() {
    private val TAG = CopyInformationActivity::class.java.simpleName

    private var record: RecordInfo? = null
    private var orgID: Int? = null
    private var groupCopiesBySystem = false
    private var lv: ListView? = null
    private val copyInfoRecords = ArrayList<CopyLocationCounts>()
    private var listAdapter: CopyInformationArrayAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.initialize(this)
        if (!App.isStarted()) {
            App.restartApp(this)
            return
        }

        setContentView(R.layout.copy_information_list)

        ActionBarUtils.initActionBarForActivity(this)

        if (savedInstanceState != null) {
            record = savedInstanceState.getSerializable("recordInfo") as RecordInfo
            orgID = savedInstanceState.getInt("orgID")
        } else {
            record = intent.getSerializableExtra("recordInfo") as RecordInfo
            orgID = intent.getIntExtra("orgID", 1)
        }
        groupCopiesBySystem = resources.getBoolean(R.bool.ou_group_copy_info_by_system)

        lv = findViewById(R.id.copy_information_list)
        listAdapter = CopyInformationArrayAdapter(this, R.layout.copy_information_item, copyInfoRecords)
        lv?.setAdapter(listAdapter)
        if (resources.getBoolean(R.bool.ou_enable_copy_info_web_links)) {
            lv?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
                val clc = lv?.getItemAtPosition(position) as CopyLocationCounts
                launchOrgDetails(clc.orgId)
            })
        } else {
            lv?.setSelector(android.R.color.transparent)
        }

        val summaryText = findViewById<View>(R.id.copy_information_summary) as TextView
        summaryText.text = RecordLoader.getCopySummary(record, orgID!!, this)

        initCopyLocationCounts()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("recordInfo", record)
        outState.putInt("orgID", orgID!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun launchOrgDetails(orgID: Int?) {
        val intent = Intent(this, OrgDetailsActivity::class.java)
        intent.putExtra("orgID", orgID)
        startActivity(intent)
    }

    private fun updateCopyInfo(copyLocationCountsList: List<CopyLocationCounts>?) {
        if (copyLocationCountsList == null) return
        copyInfoRecords.clear()
        for (clc in copyLocationCountsList) {
            val org = findOrg(clc.orgId)
            // if a branch is not opac_visible, its copies should not be visible
            if (org != null && org.opac_visible) {
                copyInfoRecords!!.add(clc)
            }
        }
        if (groupCopiesBySystem) {
            // sort by system, then by branch, like http://gapines.org/eg/opac/record/5700567?locg=1
            copyInfoRecords.sortWith(Comparator { a, b ->
                val aOrg = findOrg(a.orgId)
                val bOrg = findOrg(b.orgId)
                val aSystemName = getOrgNameSafe(aOrg?.parent_ou)
                val bSystemName = getOrgNameSafe(bOrg?.parent_ou)
                val compareBySystem = compareValues(aSystemName, bSystemName)
                if (compareBySystem != 0) compareBySystem else compareValues(aOrg?.name, bOrg?.name)
            })
        } else {
            copyInfoRecords.sortWith(Comparator { a, b ->
                compareValues(getOrgNameSafe(a.orgId), getOrgNameSafe(b.orgId))
            })
        }
        listAdapter?.notifyDataSetChanged()
    }

    private fun initCopyLocationCounts() {
        val start_ms = System.currentTimeMillis()
        val org = findOrg(orgID)
        val url = buildUrl(Api.SEARCH, Api.COPY_LOCATION_COUNTS, arrayOf(record!!.doc_id, org!!.id, org.level))
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                { response ->
                    val duration_ms = System.currentTimeMillis() - start_ms
                    Log.d(TAG, "fetch " + record!!.doc_id + " took " + duration_ms + "ms")
                    updateCopyInfo(RecordInfo.parseCopyLocationCounts(record, response))
                },
                { error ->
                    Log.d(TAG, "caught", error)
                    updateCopyInfo(RecordInfo.parseCopyLocationCounts(record, null))
                })
        Volley.getInstance(this).addToRequestQueue(r)
    }

    internal inner class CopyInformationArrayAdapter(context: Context, private val resourceId: Int, private val records: List<CopyLocationCounts>) : ArrayAdapter<CopyLocationCounts>(context, resourceId, records) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val row = when(convertView) {
                null -> {
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    inflater.inflate(resourceId, parent, false)
                }
                else -> {
                    convertView
                }
            }

            val majorLocationText = row.findViewById<TextView>(R.id.copy_information_major_location)
            val minorLocationText = row.findViewById<TextView>(R.id.copy_information_minor_location)
            val copyCallNumberText = row.findViewById<TextView>(R.id.copy_information_call_number)
            val copyLocationText = row.findViewById<TextView>(R.id.copy_information_copy_location)
            val copyStatusesText = row.findViewById<TextView>(R.id.copy_information_statuses)

            val clc = getItem(position)
            val org = findOrg(clc.orgId)

            if (groupCopiesBySystem) {
                majorLocationText.text = getOrgNameSafe(org!!.parent_ou)
                val ss = SpannableString(org.name)
                ss.setSpan(URLSpan(""), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                minorLocationText.setText(ss, TextView.BufferType.SPANNABLE)
                minorLocationText.setOnClickListener { launchOrgDetails(org.id) }
            } else {
                majorLocationText.text = getOrgNameSafe(clc.orgId)
                minorLocationText.visibility = View.GONE
            }
            copyCallNumberText.text = clc.callNumber
            copyLocationText.text = clc.copyLocation
            copyStatusesText.text = clc.countsByStatusLabel

            return row
        }
    }
}
