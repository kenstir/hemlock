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
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.CopyLocationCounts
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.Appx
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.Key
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.logBundleSize
import net.kenstir.ui.util.setMargins
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.OrgDetailsActivity
import net.kenstir.ui.view.holds.PlaceHoldActivity
import net.kenstir.util.getCopySummary
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgOrg.findOrg
import org.evergreen_ils.system.EgOrg.getOrgNameSafe

class CopyInformationActivity : BaseActivity() {

    private lateinit var record: BibRecord
    private var orgID: Int = EgOrg.consortiumID
    private var placeHoldButton: Button? = null
    private val copyInfoRecords = ArrayList<CopyLocationCounts>()
    private var rv: RecyclerView? = null
    private var adapter: CopyInformationViewAdapter? = null

    private val groupCopiesBySystem: Boolean
        get() = resources.getBoolean(R.bool.ou_group_copy_info_by_system)

    override fun adjustPaddingForEdgeToEdge() {
        super.adjustPaddingForEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.floating_action_button)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setMargins(bottom = systemBars.bottom)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_copy_information)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        if (savedInstanceState != null) {
            record = savedInstanceState.getSerializable(Key.RECORD_INFO) as BibRecord
            orgID = savedInstanceState.getInt(Key.ORG_ID)
        } else {
            record = intent.getSerializableExtra(Key.RECORD_INFO) as BibRecord
            orgID = intent.getIntExtra(Key.ORG_ID, EgOrg.consortiumID)
        }

        rv = findViewById(R.id.recycler_view)
        adapter = CopyInformationViewAdapter(copyInfoRecords, groupCopiesBySystem)
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        initClickListener()

        val summaryText = findViewById<View>(R.id.copy_information_summary) as TextView
        summaryText.text = record.getCopySummary(resources, orgID)

        placeHoldButton = findViewById(R.id.floating_action_button)
        placeHoldButton?.setOnClickListener {
            val intent = Intent(this, PlaceHoldActivity::class.java)
            intent.putExtra(Key.RECORD_INFO, record)
            startActivity(intent)
        }
    }

    private fun initClickListener() {
        if (resources.getBoolean(R.bool.ou_enable_copy_info_web_links)) {
            ItemClickSupport.addTo(rv ?: return).setOnItemClickListener { _, position, _ ->
                val clc = copyInfoRecords[position]
                launchOrgDetails(clc.orgId)
            }
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
        logBundleSize(outState)
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

    private fun updateCopyInfo(copyLocationCountsList: List<CopyLocationCounts>) {
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
        adapter?.notifyDataSetChanged()
    }

    private fun fetchData() {
        scope.async {
            try {
                val org = findOrg(orgID) ?: return@async
                val result = Appx.svc.searchService.fetchCopyLocationCounts(record.id, org.id, org.level)
                if (result is Result.Error) { showAlert(result.exception); return@async }
                updateCopyInfo(result.get())
            } catch (ex: Exception) {
                showAlert(ex)
            }
        }
    }

    companion object {
        private const val TAG = "CopyInformationActivity"
    }
}
