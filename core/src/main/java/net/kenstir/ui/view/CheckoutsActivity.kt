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
package net.kenstir.ui.view

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.CircRecord
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.history.HistoryActivity
import net.kenstir.ui.view.search.RecordDetails

class CheckoutsActivity : BaseActivity() {
    private val TAG = "Checkouts"

    private var lv: ListView? = null
    private var listAdapter: CheckoutsArrayAdapter? = null
    private var circRecords = mutableListOf<CircRecord>()
    private var progress: net.kenstir.ui.util.ProgressDialogSupport? = null
    private var checkoutsSummary: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_checkouts)

        checkoutsSummary = findViewById(R.id.checkout_items_summary)
        progress = net.kenstir.ui.util.ProgressDialogSupport()
        lv = findViewById(R.id.checkout_items_list)
        listAdapter = CheckoutsArrayAdapter(this, R.layout.checkout_list_item)
        lv?.adapter = listAdapter
        lv?.setOnItemClickListener { _, _, position, _ -> onItemClick(position) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_checkouts, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> {
                if (net.kenstir.ui.App.getAccount().circHistoryStart != null) {
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
                val result = net.kenstir.ui.App.getServiceConfig().userService.enableCheckoutHistory(
                    net.kenstir.ui.App.getAccount())
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
                val account = net.kenstir.ui.App.getAccount()
                val circService = net.kenstir.ui.App.getServiceConfig().circService

                // fetch checkouts
                val result = circService.fetchCheckouts(account)
                if (result is Result.Error) { showAlert(result.exception); return@async }
                circRecords = result.get().toMutableList()

                // fetch details
                var jobs = mutableListOf<Job>()
                for (circRecord in circRecords) {
                    jobs.add(scope.async { circService.loadCheckoutDetails(account, circRecord) })
                }
                checkoutsSummary?.text = String.format(getString(R.string.checkout_items), circRecords.size)

                jobs.joinAll()
                updateCheckoutsList()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updateCheckoutsList() {
        listAdapter?.clear()
        circRecords.sortBy { it.dueDate }
        listAdapter?.addAll(circRecords)
        listAdapter?.notifyDataSetChanged()
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

    internal inner class CheckoutsArrayAdapter(context: Context, private val resourceId: Int) : ArrayAdapter<CircRecord>(context, resourceId) {
        private var title: TextView? = null
        private var author: TextView? = null
        private var format: TextView? = null
        private var renewals: TextView? = null
        private var dueDate: TextView? = null
        private var renewButton: TextView? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row = when(convertView) {
                null -> {
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    inflater.inflate(resourceId, parent, false)
                }
                else -> {
                    convertView
                }
            }

            title = row.findViewById(R.id.checkout_record_title)
            author = row.findViewById(R.id.checkout_record_author)
            format = row.findViewById(R.id.checkout_record_format)
            renewals = row.findViewById(R.id.checkout_record_renewals)
            dueDate = row.findViewById(R.id.checkout_record_due_date)
            renewButton = row.findViewById(R.id.renew_button)

            val record = getItem(position)!!
            title?.text = record.title
            author?.text = record.author
            format?.text = record.record?.iconFormatLabel
            renewals?.text = String.format(getString(R.string.checkout_renewals_left), record.renewals)
            dueDate?.text = dueDateText(record)

            initRenewButton(record)
            maybeHighlightDueDate(record)

            return row
        }

        private fun dueDateText(record: CircRecord): String {
            return when {
                record.isOverdue ->
                    String.format(getString(R.string.label_due_date_overdue), record.dueDateLabel)
                record.isDueSoon && record.autoRenewals > 0 ->
                    String.format(getString(R.string.label_due_date_may_autorenew), record.dueDateLabel)
                record.wasAutorenewed ->
                    String.format(getString(R.string.label_due_date_autorenewed), record.dueDateLabel)
                else ->
                    String.format(getString(R.string.label_due_date), record.dueDateLabel)
            }
        }

        private fun maybeHighlightDueDate(record: CircRecord) {
            val style = when {
                record.isOverdue -> R.style.alertText
                record.isDueSoon -> R.style.warningText
                else -> R.style.HemlockText_ListTertiary
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dueDate?.setTextAppearance(style)
            } else {
                dueDate?.setTextAppearance(applicationContext, style)
            }
        }

        private fun initRenewButton(record: CircRecord) {
            val renewable = record.renewals > 0
            renewButton?.isEnabled = renewable
            renewButton?.setOnClickListener(View.OnClickListener {
                if (!renewable) return@OnClickListener
                val builder = AlertDialog.Builder(this@CheckoutsActivity)
                builder.setMessage(R.string.renew_item_message)
                builder.setNegativeButton(android.R.string.no, null)
                builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                    //Analytics.logEvent("checkouts_renewitem", "num_renewals", record.renewals, "overdue", record.isOverdue)
                    renewItem(record)
                }
                builder.create().show()
            })
        }
    }

    private fun renewItem(record: CircRecord) {
        scope.async {
//            Log.d(TAG, "[kcxxx] renewItem: ${record.targetCopy}")

            record.targetCopy?.let {
                progress?.show(this@CheckoutsActivity, getString(R.string.msg_renewing_item))
                val result = net.kenstir.ui.App.getServiceConfig().circService.renewCheckout(
                    net.kenstir.ui.App.getAccount(), it)
                progress?.dismiss()
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@CheckoutsActivity, getString(R.string.toast_item_renewed), Toast.LENGTH_LONG).show()
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
