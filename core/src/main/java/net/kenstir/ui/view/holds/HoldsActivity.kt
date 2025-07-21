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
package net.kenstir.ui.view.holds

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.data.Result
import net.kenstir.ui.Key
import net.kenstir.logging.Log
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.search.RecordDetails
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.HoldRecord
import net.kenstir.ui.App
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import java.util.ArrayList

class HoldsActivity : BaseActivity() {
    private var lv: ListView? = null
    private var listAdapter: HoldsArrayAdapter? = null
    private var holdRecords = listOf<HoldRecord>()
    private var holdsSummary: TextView? = null
    private var progress: ProgressDialogSupport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_holds)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        holdsSummary = findViewById(R.id.holds_summary)
        lv = findViewById(R.id.list_view)

        progress = ProgressDialogSupport()
        listAdapter = HoldsArrayAdapter(this, R.layout.holds_list_item)
        lv?.adapter = listAdapter
        lv?.setOnItemClickListener { _, _, position, _ ->
            showItemDetails(position)
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        fetchData()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            HoldDetailsActivity.RESULT_CODE_CANCEL ->
                Log.d(TAG, "Do nothing")
            HoldDetailsActivity.RESULT_CODE_DELETE_HOLD,
            HoldDetailsActivity.RESULT_CODE_UPDATE_HOLD -> {
                Log.d(TAG, "Update on result $resultCode")
                fetchData()
            }
        }
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Deferred<Any>>()
                progress?.show(this@HoldsActivity, getString(R.string.msg_loading_holds))

                // fetchHolds
                val result = App.getServiceConfig().circService.fetchHolds(
                    App.getAccount())
                when (result) {
                    is Result.Success ->
                        holdRecords = result.get()
                    is Result.Error -> {
                        showAlert(result.exception)
                        return@async
                    }
                }
                holdsSummary?.text = String.format(getString(R.string.n_items_on_hold), holdRecords.size)

                // fetch hold target details and queue stats
                for (hold in holdRecords) {
                    jobs.add(scope.async {
                        App.getServiceConfig().circService.loadHoldDetails(
                            App.getAccount(), hold)
                    })
                }

                jobs.map { it.await() }
                updateHoldsList()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updateHoldsList() {
        listAdapter?.clear()
        listAdapter?.addAll(holdRecords)
        listAdapter?.notifyDataSetChanged()
    }

    private fun editHold(record: HoldRecord) {
        val intent = Intent(applicationContext, HoldDetailsActivity::class.java)
        intent.putExtra(Key.HOLD_RECORD, record)
        // request code does not matter, but we use the result code
        startActivityForResult(intent, 0)
    }

    private fun showItemDetails(position: Int) {
        val records = ArrayList<BibRecord>()
        for (hold in holdRecords) {
            hold.record?.let { record ->
                records.add(record)
            }
        }
        if (records.isNotEmpty()) {
            RecordDetails.launchDetailsFlow(this@HoldsActivity, records, position)
        }
    }

    internal inner class HoldsArrayAdapter(context: Context, private val resourceId: Int) :
            ArrayAdapter<HoldRecord>(context, resourceId) {

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

            val titleText = row.findViewById<TextView>(R.id.hold_title)
            val authorText = row.findViewById<TextView>(R.id.hold_author)
            val formatText = row.findViewById<TextView>(R.id.hold_format)
            val statusText = row.findViewById<TextView>(R.id.hold_status)
            val editButton = row.findViewById<Button>(R.id.edit_button)

            val record = getItem(position)
            titleText?.text = record?.title
            authorText?.text = record?.author
            formatText?.text = record?.formatLabel
            statusText?.text = record?.getHoldStatus(resources)

            initEditButton(editButton, record)

            return row
        }

        // This one-line function is necessary; doing setOnClickListener inside getView()
        // captures the wrong [record], and all edit buttons operate on the last hold.
        fun initEditButton(editButton: Button?, record: HoldRecord?) {
            editButton?.setOnClickListener {
                if (record != null) {
                    editHold(record)
                }
            }
        }
    }

    companion object {
        val TAG = HoldsActivity::class.java.simpleName
    }
}
