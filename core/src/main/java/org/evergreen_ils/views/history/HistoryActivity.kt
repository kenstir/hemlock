/*
 * Copyright (c) 2024 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.views.history

import android.app.AlertDialog
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.android.ui.ItemClickSupport
import net.kenstir.hemlock.android.ui.ProgressDialogSupport
import net.kenstir.hemlock.android.ui.showAlert
import org.evergreen_ils.data.HistoryRecord
import org.evergreen_ils.data.MBRecord
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.ui.*
import org.evergreen_ils.views.search.DividerItemDecoration
import org.evergreen_ils.views.search.RecordDetails

class HistoryActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    private var historySummary: TextView? = null
    private var rv: RecyclerView? = null
    private var adapter: HistoryViewAdapter? = null
    private var items = ArrayList<HistoryRecord>()
    private var progress: ProgressDialogSupport? = null
    private var contextMenuInfo: ContextMenuItemInfo? = null

    private class ContextMenuItemInfo(val position: Int, val item: HistoryRecord) : ContextMenu.ContextMenuInfo {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_history)
        progress = ProgressDialogSupport()

        historySummary = findViewById(R.id.history_items_summary)
        rv = findViewById(R.id.recycler_view)
        adapter = HistoryViewAdapter(items)
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))

        initClickListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_disable_history -> {
                maybeDisableHistory()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun maybeDisableHistory() {
        val builder = AlertDialog.Builder(this@HistoryActivity)
        builder.setTitle(getString(R.string.disable_history_alert_title))
            .setMessage(getString(R.string.disable_history_alert_msg))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .setPositiveButton(getString(R.string.disable_history_alert_button)) { _, _ ->
                disableCheckoutHistory()
            }
        builder.create().show()
    }

    private fun disableCheckoutHistory() {
        scope.async {
            try {
                // first disable the patron setting
                val result = Gateway.actor.disableCheckoutHistory(App.getAccount())
                if (result is Result.Error) { showAlert(result.exception); return@async }

                // then clear history
                val clearResult = Gateway.actor.clearCheckoutHistory(App.getAccount(), null)
                if (clearResult is Result.Error) { showAlert(clearResult.exception); return@async }

                finish()
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
                progress?.show(this@HistoryActivity, getString(R.string.msg_retrieving_data))

                // fetch history
                val result = Gateway.actor.fetchCheckoutHistory(App.getAccount())
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
                val objects = result.get()

                val historyList = HistoryRecord.makeArray(objects)
                loadHistory(historyList)
                historySummary?.text = String.format(getString(R.string.history_items), historyList.size, App.getAccount().circHistoryStart)
                updateList()

                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun loadHistory(historyList: List<HistoryRecord>) {
        items.clear()
        items.addAll(historyList)
    }

    private fun updateList() {
        adapter?.notifyDataSetChanged()
    }

    private fun initClickListener() {
        registerForContextMenu(rv)
        val cs = ItemClickSupport.addTo(rv)
        cs.setOnItemClickListener { _, position, _ ->
            viewItemAtPosition(position)
        }
//        cs.setOnItemLongClickListener { recyclerView, position, _ ->
//            contextMenuInfo = ContextMenuMessageInfo(position, items[position])
//            openContextMenu(recyclerView)
//            return@setOnItemLongClickListener true
//        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (v.id == R.id.recycler_view) {
            // TODO
            Log.d(TAG, "stop here")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = contextMenuInfo ?: return super.onContextItemSelected(item)
//        when (item.itemId) {
//            MESSAGE_DELETE -> {
//                markMessageDeleted(info.message)
//                return true
//            }
//            MESSAGE_VIEW -> {
//                viewMessage(info.message)
//                return true
//            }
//        }
        return super.onContextItemSelected(item)
    }

    private fun viewItemAtPosition(position: Int) {
        // The history list may be quite long; just look at this one item, or else we risk
        // a TransactionTooLargeException.
        val records = ArrayList<MBRecord>()
        items[position].record?.let { record ->
            if (record.id != -1) {
                records.add(record)
                RecordDetails.launchDetailsFlow(this@HistoryActivity, records, 0)
            }
        }
    }
}
