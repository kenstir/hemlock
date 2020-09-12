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
package org.evergreen_ils.views

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.data.CircRecord
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.searchCatalog.RecordDetails
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.android.Log
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import java.util.*

class CheckoutsActivity : BaseActivity() {
    private val TAG = "Checkouts"

    private var lv: ListView? = null
    private var listAdapter: CheckoutsArrayAdapter? = null
    private var circRecords: ArrayList<CircRecord> = ArrayList()
    private var progress: ProgressDialogSupport? = null
    private var checkoutsSummary: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_checkouts)

        checkoutsSummary = findViewById(R.id.checkout_items_summary)
        progress = ProgressDialogSupport()
        lv = findViewById(R.id.checkout_items_list)
        circRecords = ArrayList()
        listAdapter = CheckoutsArrayAdapter(this, R.layout.checkout_list_item, circRecords)
        lv?.adapter = listAdapter
        lv?.setOnItemClickListener { parent, view, position, id -> onItemClick(position) }
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

    private fun fetchData() {
        async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@CheckoutsActivity, getString(R.string.msg_retrieving_data))

                // fetch checkouts
                val result = Gateway.actor.fetchUserCheckedOut(App.getAccount())
                if (result is Result.Error) { showAlert(result.exception); return@async }
                val obj = result.get()

                // fetch details
                circRecords = CircRecord.makeArray(obj)
                for (circRecord in circRecords) {
                    jobs.add(async { fetchCircDetails(circRecord) })
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

    private suspend fun fetchCircDetails(circRecord: CircRecord): Result<Unit> {
        val circResult = Gateway.circ.fetchCirc(App.getAccount(), circRecord.circId)
        if (circResult is Result.Error) return circResult
        val circObj = circResult.get()
        circRecord.circ = circObj

        val targetCopy = circObj.getInt("target_copy") ?: return Result.Error(Exception("circ item has no target_copy"))
        val modsResult = Gateway.search.fetchCopyMODS(targetCopy)
        if (modsResult is Result.Error) return modsResult
        val modsObj = modsResult.get()
        val record = RecordInfo(modsObj)
        circRecord.recordInfo = record

        if (record.doc_id != -1) {
            val mraResult = fetchRecordAttrs(record, record.doc_id)
        }

        return Result.Success(Unit)
    }

    suspend fun fetchRecordAttrs(record: RecordInfo, id: Int): Result<Unit> {
        val mraResult = Gateway.pcrud.fetchMRA(id)
        if (mraResult is Result.Error) return mraResult
        val mraObj = mraResult.get()
        record.updateFromMRAResponse(mraObj)
        return Result.Success(Unit)
    }

    private fun updateCheckoutsList() {
        listAdapter?.clear()
        circRecords.sortBy { it.dueDate }
        for (circ in circRecords) listAdapter?.add(circ)
        listAdapter?.notifyDataSetChanged()
    }

    private fun onItemClick(position: Int) {
        val records = ArrayList<RecordInfo?>()
        for (circRecord in circRecords) {
            if (circRecord.recordInfo?.doc_id != -1) {
                records.add(circRecord.recordInfo)
            }
        }
        if (records.isNotEmpty()) {
            RecordDetails.launchDetailsFlow(this@CheckoutsActivity, records, position)
        }
    }

    internal inner class CheckoutsArrayAdapter(context: Context, private val resourceId: Int, private val items: List<CircRecord>) : ArrayAdapter<CircRecord>(context, resourceId, items) {
        private var title: TextView? = null
        private var author: TextView? = null
        private var format: TextView? = null
        private var renewals: TextView? = null
        private var dueDate: TextView? = null
        private var renewButton: TextView? = null

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(index: Int): CircRecord {
            return items[index]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
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

            val record = getItem(position)
            title?.text = record.title
            author?.text = record.author
            format?.text = RecordInfo.getIconFormatLabel(record.recordInfo)
            renewals?.text = String.format(getString(R.string.checkout_renewals_left), record.renewals)
            dueDate?.text = dueDateText(record)

            initRenewButton(record)
            maybeHighlightDueDate(record)

            return row
        }

        private fun dueDateText(record: CircRecord): String {
            return when {
                record.isOverdue ->
                    String.format(getString(R.string.label_due_date_overdue), record.dueDateString)
                record.isDue && record.autoRenewals > 0 ->
                    String.format(getString(R.string.label_due_date_may_autorenew), record.dueDateString)
                record.wasAutorenewed && !record.isDue ->
                    String.format(getString(R.string.label_due_date_autorenewed), record.dueDateString)
                else ->
                    String.format(getString(R.string.label_due_date), record.dueDateString)
            }
        }

        private fun maybeHighlightDueDate(record: CircRecord) {
            val style = if (record.isDue) R.style.alertText else R.style.HemlockText_ListTertiary
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
        async {
//            Log.d(TAG, "[kcxxx] renewItem: ${record.targetCopy}")

            record.targetCopy?.let {
                progress?.show(this@CheckoutsActivity, getString(R.string.msg_renewing_item))
                val result = Gateway.circ.renewCircAsync(App.getAccount(), it)
                progress?.dismiss()
                when (result) {
                    is Result.Success -> {
                        // The response is a SUCCESS event, but we just care that it isn't an error
//                        Log.d(TAG, "[kcxxx] ${result.data}")
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
