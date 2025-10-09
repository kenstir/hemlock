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

package net.kenstir.ui.view.messages

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.ui.Key
import net.kenstir.ui.util.showAlert
import net.kenstir.data.model.PatronMessage
import net.kenstir.data.Result
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.view.search.DividerItemDecoration

const val MESSAGE_DELETE = 0
const val MESSAGE_MARK_READ = 1
const val MESSAGE_MARK_UNREAD = 2
const val MESSAGE_VIEW = 3

class MessagesActivity : BaseActivity() {

    private var rv: RecyclerView? = null
    private var adapter: MessageViewAdapter? = null
    private var items = ArrayList<PatronMessage>()
    private var progress: ProgressDialogSupport? = null
    private var contextMenuInfo: ContextMenuMessageInfo? = null

    private class ContextMenuMessageInfo(val position: Int, val message: PatronMessage) : ContextMenu.ContextMenuInfo {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_messages)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        progress = ProgressDialogSupport()

        rv = findViewById(R.id.recycler_view)
        adapter = MessageViewAdapter(items)
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))

        initClickListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@MessagesActivity, getString(R.string.msg_retrieving_data))

                // fetch messages
                val result = App.getServiceConfig().userService.fetchPatronMessages(
                    App.getAccount())
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
                val messageList = result.get()

                loadVisibleMessages(messageList)

                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun loadVisibleMessages(messages: List<PatronMessage>) {
        items.clear()
        messages.forEach {
            if (it.isPatronVisible && !it.isDeleted) {
                items.add(it)
            }
        }
        adapter?.notifyDataSetChanged()
    }

    private fun initClickListener() {
        registerForContextMenu(rv)
        val cs = ItemClickSupport.addTo(rv)
        cs.setOnItemClickListener { _, position, _ ->
            if (position < 0 || position >= items.size) return@setOnItemClickListener
            viewMessage(items[position])
        }
        cs.setOnItemLongClickListener { recyclerView, position, _ ->
            if (position < 0 || position >= items.size) return@setOnItemLongClickListener false
            contextMenuInfo = ContextMenuMessageInfo(position, items[position])
            openContextMenu(recyclerView)
            return@setOnItemLongClickListener true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            fetchData()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (v.id == R.id.recycler_view) {
            menu.add(Menu.NONE, MESSAGE_DELETE, 3, getString(R.string.menu_delete_message))
            menu.add(Menu.NONE, MESSAGE_MARK_READ, 1, getString(R.string.menu_mark_read))
            menu.add(Menu.NONE, MESSAGE_MARK_UNREAD, 2, getString(R.string.menu_mark_unread))
            menu.add(Menu.NONE, MESSAGE_VIEW, 0, getString(R.string.menu_view_message))
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = contextMenuInfo ?: return super.onContextItemSelected(item)
        when (item.itemId) {
            MESSAGE_DELETE -> {
                markMessageDeleted(info.message)
                return true
            }
            MESSAGE_MARK_READ -> {
                markMessageRead(info.message)
                return true
            }
            MESSAGE_MARK_UNREAD -> {
                markMessageUnread(info.message)
                return true
            }
            MESSAGE_VIEW -> {
                viewMessage(info.message)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun viewMessage(message: PatronMessage) {
        val intent = Intent(this, MessageDetailsActivity::class.java)
        intent.putExtra(Key.PATRON_MESSAGE, message)
        startActivityForResult(intent, 0)
    }

    private fun markMessageDeleted(message: PatronMessage) {
        scope.async {
            val result = App.getServiceConfig().userService.markMessageDeleted(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            fetchData()
        }
    }

    private fun markMessageRead(message: PatronMessage) {
        scope.async {
            val result = App.getServiceConfig().userService.markMessageRead(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            fetchData()
        }
    }

    private fun markMessageUnread(message: PatronMessage) {
        scope.async {
            val result = App.getServiceConfig().userService.markMessageUnread(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            fetchData()
        }
    }

    companion object {
        private const val TAG = "MessagesActivity"

        const val RESULT_MESSAGE_UPDATED = 1
    }
}
