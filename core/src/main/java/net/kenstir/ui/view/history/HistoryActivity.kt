/*
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

package net.kenstir.ui.view.history

import android.app.AlertDialog
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.HistoryRecord
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.search.DividerItemDecoration
import net.kenstir.ui.view.search.RecordDetails

class HistoryActivity : BaseActivity() {

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

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_history)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

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
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(getString(R.string.disable_history_alert_button)) { _, _ ->
                disableCheckoutHistory()
            }
        builder.create().show()
    }

    private fun disableCheckoutHistory() {
        scope.async {
            try {
                // first disable the patron setting
                val result = App.getServiceConfig().userService.disableCheckoutHistory(
                    App.getAccount())
                if (result is Result.Error) { showAlert(result.exception); return@async }

                // then clear history
                val clearResult = App.getServiceConfig().userService.clearCheckoutHistory(
                    App.getAccount())
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
                Log.d(TAG, "[fetch] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@HistoryActivity, getString(R.string.msg_retrieving_data))

                // fetch history
                val result = App.getServiceConfig().circService.fetchCheckoutHistory(
                    App.getAccount())
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
                loadHistory(result.get())

                Log.logElapsedTime(TAG, start, "[fetch] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun loadHistory(historyList: List<HistoryRecord>) {
        historySummary?.text = String.format(getString(R.string.history_items), historyList.size, App.getAccount().circHistoryStart)

        items.clear()
        items.addAll(historyList)
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
        val records = ArrayList<BibRecord>()
        items[position].record?.let { record ->
            if (record.id != -1) {
                records.add(record)
                RecordDetails.launchDetailsFlow(this@HistoryActivity, records, 0)
            }
        }
    }

    companion object {
        private const val TAG = "HistoryActivity"
    }
}
