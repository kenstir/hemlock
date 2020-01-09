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
package org.evergreen_ils.accountAccess.holds

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.Log
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import org.opensrf.ShouldNotHappenException

class HoldsActivity : BaseActivity() {
    private var lv: ListView? = null
    private var listAdapter: HoldsArrayAdapter? = null
    private var holdRecords = mutableListOf<HoldRecord>()
    private var getHoldsRunnable: Runnable? = null
    private var holdsSummary: TextView? = null
    private var progress: ProgressDialogSupport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return
        setContentView(R.layout.activity_holds)
        holdsSummary = findViewById(R.id.holds_summary)
        lv = findViewById(R.id.holds_item_list)
        progress = ProgressDialogSupport()
        //holdRecords =
        listAdapter = HoldsArrayAdapter(this, R.layout.holds_list_item, holdRecords)
        lv?.setAdapter(listAdapter)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            HoldDetailsActivity.RESULT_CODE_CANCEL -> Log.d(TAG, "Do nothing")
            HoldDetailsActivity.RESULT_CODE_DELETE_HOLD, HoldDetailsActivity.RESULT_CODE_UPDATE_HOLD -> {
                Log.d(TAG, "Update on result $resultCode")
                fetchData()
            }
        }
    }

    private fun fetchData() {
        async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@HoldsActivity, getString(R.string.msg_loading_holds))

                // fetchHolds
                val result = Gateway.circ.fetchHolds(App.getAccount())
                when (result) {
                    is Result.Success ->
                        holdRecords = HoldRecord.makeArray(result.data)
                    is Result.Error -> {
                        showAlert(result.exception)
                        return@async
                    }
                }
                holdsSummary?.text = String.format(getString(R.string.n_items_on_hold), holdRecords.size)

                // fetch hold target details and queue stats
                for (hold in holdRecords) {
                    jobs.add(async {
                        fetchHoldTargetDetails(hold, App.getAccount())
                        // we wait until we have a RecordInfo so we have a place to store the attrs
                        hold.recordInfo?.doc_id?.let { id ->
                            fetchRecordAttrs(hold.recordInfo, id)
                        }
                    })
                    jobs.add(async {
                        fetchHoldQueueStats(hold, App.getAccount())
                    })
                }

                jobs.joinAll()
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

    suspend fun fetchRecordAttrs(record: RecordInfo, id: Int): Result<Unit> {
        val result = Gateway.pcrud.fetchMRA(id)
        return when (result) {
            is Result.Success -> {
                record.updateFromMRAResponse(result.data)
                return Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    suspend fun fetchHoldQueueStats(hold: HoldRecord, account: Account): Result<Unit> {
        val id = hold.ahr.getInt("id") ?: return Result.Error(GatewayError("null hold id"))
        val result = Gateway.circ.fetchHoldQueueStats(account, id)
        return when (result) {
            is Result.Success -> {
                hold.qstatsObj = result.data
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    suspend fun fetchHoldTargetDetails(hold: HoldRecord, account: Account): Result<Unit> {
        val target = hold.target ?: return Result.Error(GatewayError("null hold target"))
        return when (hold.holdType) {
            "T" -> fetchTitleHoldTargetDetails(hold, target, account)
            "M" -> fetchMetarecordHoldTargetDetails(hold, target, account)
            "P" -> fetchPartHoldTargetDetails(hold, target, account)
            "C" -> fetchCopyHoldTargetDetails(hold, target, account)
            "V" -> fetchVolumeHoldTargetDetails(hold, target, account)
            else -> {
                Analytics.logException(ShouldNotHappenException("unexpected holdType:${hold.holdType}"))
                Result.Error(GatewayError("unexpected hold type: ${hold.holdType}"))
            }
        }
    }

    private suspend fun fetchTitleHoldTargetDetails(hold: HoldRecord, target: Int, account: Account): Result<Unit> {
        val result = Gateway.search.fetchRecordMODS(target)
        return when (result) {
            is Result.Success -> {
                hold.recordInfo = RecordInfo(result.data)
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    private fun fetchMetarecordHoldTargetDetails(hold: HoldRecord, target: Int, account: Account): Result<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun fetchPartHoldTargetDetails(hold: HoldRecord, target: Int, account: Account): Result<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun fetchCopyHoldTargetDetails(hold: HoldRecord, target: Int, account: Account): Result<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun fetchVolumeHoldTargetDetails(hold: HoldRecord, target: Int, account: Account): Result<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun updateHoldsList() {
        listAdapter?.clear()
        for (hold in holdRecords) {
            listAdapter?.add(hold)
        }
        listAdapter?.notifyDataSetChanged()
    }

    private fun onItemClick(position: Int) {
        Analytics.logEvent("Holds: Tap List Item")
        val record = lv?.getItemAtPosition(position) as? HoldRecord
        if (record != null) {
            val intent = Intent(applicationContext, HoldDetailsActivity::class.java)
            intent.putExtra("holdRecord", record)
            // request code does not matter, but we use the result code
            startActivityForResult(intent, 0)
        }
    }

    internal inner class HoldsArrayAdapter(context: Context, private val resourceId: Int, private val items: List<HoldRecord>) :
            ArrayAdapter<HoldRecord>(context, resourceId, items) {
        private var holdTitle: TextView? = null
        private var holdAuthor: TextView? = null
        private var holdFormat: TextView? = null
        private var status: TextView? = null

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(index: Int): HoldRecord {
            return items[index]
        }

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

            holdTitle = row.findViewById(R.id.hold_title)
            holdAuthor = row.findViewById(R.id.hold_author)
            holdFormat = row.findViewById(R.id.hold_format)
            status = row.findViewById(R.id.hold_status)

            val record = getItem(position)
            holdTitle?.setText(record.title)
            holdAuthor?.setText(record.author)
            holdFormat?.setText(RecordInfo.getIconFormatLabel(record.recordInfo))
            status?.setText(record.getHoldStatus(resources))

            return row
        }
    }

    companion object {
        private val TAG = HoldsActivity::class.java.simpleName
    }
}
