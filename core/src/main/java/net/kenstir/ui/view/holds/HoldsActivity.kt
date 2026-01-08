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

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.HoldRecord
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.Key
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.search.RecordDetails

class HoldsActivity : BaseActivity() {

    private var rv: RecyclerView? = null
    private var adapter: HoldsViewAdapter? = null
    private var holdRecords = mutableListOf<HoldRecord>()
    private var holdsSummary: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_holds)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        holdsSummary = findViewById(R.id.holds_summary)
        rv = findViewById(R.id.recycler_view)
        adapter = HoldsViewAdapter(holdRecords) { editHold(it) }
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        ItemClickSupport.addTo(rv ?: return).setOnItemClickListener { _, position, _ ->
            showItemDetails(position)
        }
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
                Log.d(TAG, "[fetch] fetchData ...")
                showBusy(R.string.msg_loading_holds)
                val start = System.currentTimeMillis()
                val account = App.account

                // fetchHolds
                val result = App.svc.circService.fetchHolds(account)
                if (result is Result.Error) {
                    showAlert(result.exception)
                    return@async
                }
                val holds = result.get()
                holdsSummary?.text = String.format(getString(R.string.n_items_on_hold), holds.size)

                // fetch hold target details and queue stats
                val jobs = mutableListOf<Deferred<Any>>()
                for (hold in holds) {
                    jobs.add(scope.async {
                        App.svc.circService.loadHoldDetails(account, hold)
                    })
                }

                jobs.map { it.await() }
                updateHoldsList(holds)
                Log.logElapsedTime(TAG, start, "[fetch] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                hideBusy()
            }
        }
    }

    private fun updateHoldsList(holds: List<HoldRecord>) {
        holdRecords.clear()
        holdRecords.addAll(holds)
        adapter?.notifyDataSetChanged()
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

    companion object {
        private const val TAG = "HoldsActivity"
    }
}
