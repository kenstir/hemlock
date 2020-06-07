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
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.util.Pair
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.android.Log
import org.evergreen_ils.system.EvergreenServer
import org.evergreen_ils.system.Organization
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.OrgArrayAdapter
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.opensrf.util.OSRFObject
import java.util.*

private const val TAG = "OrgDetailsActivity"

class OrgDetailsActivity : BaseActivity() {

    private var orgSpinner: Spinner? = null
    private lateinit var orgDetailsRunnable: Runnable
    private var progress: ProgressDialogSupport? = null

    private var orgID: Int? = null
    private var org: Organization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_org_details)

        orgID = intent.getIntExtra("orgID", 1)

        orgSpinner = findViewById(R.id.org_details_spinner)

        progress = ProgressDialogSupport()

        initOrgSpinner()
        initOrgDetailsRunnable()
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        fetchData()
    }

    private fun initOrgSpinner() {
        val defaultOrgId = AccountAccess.getInstance().homeOrgID
        val pair: Pair<ArrayList<String?>, Int> = EvergreenServer.getInstance().getOrganizationSpinnerLabelsAndSelectedIndex(defaultOrgId)
        val selectedOrgPos = pair.second
        val adapter: ArrayAdapter<String> = OrgArrayAdapter(this, R.layout.org_item_layout, pair.first, false)
        orgSpinner?.adapter = adapter
        orgSpinner?.setSelection(selectedOrgPos)
        orgSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                org = EvergreenServer.getInstance().visibleOrganizations[position]
                orgID = org?.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initOrgDetailsRunnable() {
        orgDetailsRunnable = Runnable {
            runOnUiThread { progress?.show(this, getString(R.string.msg_loading_details)) }
            val obj = AccountAccess.getInstance().getHoursOfOperation(orgID);
            runOnUiThread { onHoursLoaded(obj); progress?.dismiss() }
        }
    }

    private fun onHoursLoaded(obj: OSRFObject) {
        print("blah")
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
        Thread(orgDetailsRunnable).start()
    }
}
