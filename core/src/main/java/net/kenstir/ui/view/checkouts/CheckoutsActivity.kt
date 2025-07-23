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

package net.kenstir.ui.view.checkouts

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.CircRecord
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.history.HistoryActivity
import net.kenstir.ui.view.search.DividerItemDecoration
import net.kenstir.ui.view.search.RecordDetails

class CheckoutsActivity : BaseActivity() {
    private val TAG = "Checkouts"

    private var rv: RecyclerView? = null
    private var adapter: CheckoutsViewAdapter? = null
    private var circRecords = mutableListOf<CircRecord>()
    private var progress: ProgressDialogSupport? = null
    private var checkoutsSummary: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_checkouts)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        checkoutsSummary = findViewById(R.id.checkout_items_summary)
        progress = ProgressDialogSupport()

        rv = findViewById(R.id.recycler_view)
        adapter = CheckoutsViewAdapter(circRecords) { renewItem(it) }
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))
        ItemClickSupport.addTo(rv ?: return).setOnItemClickListener { _, position, _ ->
            onItemClick(position)
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object {}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_checkouts, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> {
                if (App.getAccount().circHistoryStart != null) {
                    startActivity(Intent(this, HistoryActivity::class.java))
                } else {
                    maybeEnableHistory()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun maybeEnableHistory() {
        val builder = AlertDialog.Builder(this@CheckoutsActivity)
        builder.setTitle(getString(R.string.enable_history_alert_title))
            .setMessage(getString(R.string.enable_history_alert_msg))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .setPositiveButton(getString(R.string.enable_history_button)) { _, _ ->
                enableCheckoutHistory()
            }
        builder.create().show()
    }

    private fun enableCheckoutHistory() {
        scope.async {
            try {
                val result = App.getServiceConfig().userService.enableCheckoutHistory(
                    App.getAccount())
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
            } catch (ex: Exception) {
                showAlert(ex)
            }
        }
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@CheckoutsActivity, getString(R.string.msg_retrieving_data))
                val account = App.getAccount()
                val circService = App.getServiceConfig().circService

                // fetch checkouts
                val result = circService.fetchCheckouts(account)
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
                val checkouts = result.get()

                // fetch details
                var jobs = mutableListOf<Job>()
                for (circRecord in checkouts) {
                    jobs.add(scope.async { circService.loadCheckoutDetails(account, circRecord) })
                }
                checkoutsSummary?.text = String.format(getString(R.string.checkout_items), checkouts.size)

                jobs.joinAll()
                updateCheckoutsList(checkouts)
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updateCheckoutsList(checkouts: List<CircRecord>) {
        circRecords.clear()
        circRecords.addAll(checkouts)
        circRecords.sortBy { it.dueDate }
        adapter?.notifyDataSetChanged()
    }

    private fun onItemClick(position: Int) {
        val records = ArrayList<BibRecord>()
        for (circRecord in circRecords) {
            circRecord.record?.let { record ->
                if (record.id != -1) {
                    records.add(record)
                }
            }
        }
        if (records.isNotEmpty()) {
            RecordDetails.launchDetailsFlow(this@CheckoutsActivity, records, position)
        }
    }

    private fun renewItem(record: CircRecord) {
        scope.async {
            record.targetCopy?.let {
                progress?.show(this@CheckoutsActivity, getString(R.string.msg_renewing_item))
                val result = App.getServiceConfig().circService.renewCheckout(
                    App.getAccount(), it)
                progress?.dismiss()
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@CheckoutsActivity,
                            getString(R.string.toast_item_renewed),
                            Toast.LENGTH_LONG).show()
                        fetchData()
                    }

                    is Result.Error -> {
                        showAlert(result.exception)
                    }
                }
            }
        }
    }
}
