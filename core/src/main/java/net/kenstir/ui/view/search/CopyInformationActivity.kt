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
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.ui.Key
import net.kenstir.ui.util.showAlert
import net.kenstir.data.Result
import net.kenstir.data.model.CopyLocationCounts
import net.kenstir.logging.Log
import org.evergreen_ils.data.model.MBRecord
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgOrg.findOrg
import org.evergreen_ils.system.EgOrg.getOrgNameSafe
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.view.OrgDetailsActivity
import net.kenstir.ui.view.holds.PlaceHoldActivity

class CopyInformationActivity : BaseActivity() {
    private val TAG = CopyInformationActivity::class.java.simpleName

    private lateinit var record: MBRecord
    private var orgID: Int = EgOrg.consortiumID
    private var lv: ListView? = null
    private var placeHoldButton: Button? = null
    private val copyInfoRecords = ArrayList<CopyLocationCounts>()
    private var listAdapter: CopyInformationArrayAdapter? = null

    private val groupCopiesBySystem: Boolean
        get() = resources.getBoolean(R.bool.ou_group_copy_info_by_system)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.copy_information_list)

        if (savedInstanceState != null) {
            record = savedInstanceState.getSerializable(Key.RECORD_INFO) as MBRecord
            orgID = savedInstanceState.getInt(Key.ORG_ID)
        } else {
            record = intent.getSerializableExtra(Key.RECORD_INFO) as MBRecord
            orgID = intent.getIntExtra(Key.ORG_ID, EgOrg.consortiumID)
        }

        lv = findViewById(R.id.copy_information_list)
        listAdapter = CopyInformationArrayAdapter(this, R.layout.copy_information_item, copyInfoRecords)
        lv?.adapter = listAdapter
        if (resources.getBoolean(R.bool.ou_enable_copy_info_web_links)) {
            lv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                val clc = lv?.getItemAtPosition(position) as CopyLocationCounts
                launchOrgDetails(clc.orgId)
            }
        } else {
            lv?.setSelector(android.R.color.transparent)
        }

        val summaryText = findViewById<View>(R.id.copy_information_summary) as TextView
        summaryText.text = record.getCopySummary(resources, orgID)

        placeHoldButton = findViewById(R.id.simple_place_hold_button)
        placeHoldButton?.setOnClickListener {
            val intent = Intent(this, PlaceHoldActivity::class.java)
            intent.putExtra(Key.RECORD_INFO, record)
            startActivity(intent)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(Key.RECORD_INFO, record)
        outState.putInt(Key.ORG_ID, orgID)
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
        intent.putExtra(Key.ORG_ID, orgID)
        startActivity(intent)
    }

    private fun updateCopyInfo(copyLocationCountsList: List<CopyLocationCounts>?) {
        if (copyLocationCountsList == null) return
        copyInfoRecords.clear()
        for (clc in copyLocationCountsList) {
            val org = findOrg(clc.orgId)
            // if a branch is not opac_visible, its copies should not be visible
            if (org != null && org.opacVisible) {
                copyInfoRecords.add(clc)
            }
        }
        if (groupCopiesBySystem) {
            // sort by system, then by branch, like http://gapines.org/eg/opac/record/5700567?locg=1
            copyInfoRecords.sortWith(Comparator { a, b ->
                val aOrg = findOrg(a.orgId)
                val bOrg = findOrg(b.orgId)
                val aSystemName = getOrgNameSafe(aOrg?.parent)
                val bSystemName = getOrgNameSafe(bOrg?.parent)
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

    private fun fetchData() {
        scope.async {
            try {
                val org = findOrg(orgID) ?: return@async
                val result = net.kenstir.ui.App.getServiceConfig().searchService.fetchCopyLocationCounts(record.id, org.id, org.level)
                if (result is Result.Error) { showAlert(result.exception); return@async }
                updateCopyInfo(result.get())
            } catch (ex: Exception) {
                showAlert(ex)
            }
        }
    }

    internal inner class CopyInformationArrayAdapter(context: Context, private val resourceId: Int, private val items: List<CopyLocationCounts>) : ArrayAdapter<CopyLocationCounts>(context, resourceId, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
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
            val org = findOrg(clc?.orgId)

            if (groupCopiesBySystem) {
                majorLocationText.text = getOrgNameSafe(org?.parent)
                val ss = SpannableString(org?.name)
                ss.setSpan(URLSpan(""), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                minorLocationText.setText(ss, TextView.BufferType.SPANNABLE)
                minorLocationText.setOnClickListener { launchOrgDetails(org?.id) }
            } else {
                majorLocationText.text = getOrgNameSafe(clc?.orgId)
                minorLocationText.visibility = View.GONE
            }
            copyCallNumberText.text = clc?.callNumber
            copyLocationText.text = clc?.copyLocation
            copyStatusesText.text = clc?.countsByStatusLabel

            return row
        }
    }
}
