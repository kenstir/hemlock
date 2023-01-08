/*
 * Copyright (c) 2023 Kenneth H. Cox
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

package org.evergreen_ils.views.messages

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.PatronMessage
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import org.evergreen_ils.views.search.DividerItemDecoration

class MessagesActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    private var rv: RecyclerView? = null
    private var adapter: MessageViewAdapter? = null
    private var items = ArrayList<PatronMessage>();
    private var progress: ProgressDialogSupport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_messages)

        progress = ProgressDialogSupport()

        rv = findViewById(R.id.recycler_view)
        adapter = MessageViewAdapter(items)
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
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
                progress?.show(this@MessagesActivity, getString(R.string.msg_retrieving_data))

                // fetch messages
                val result = Gateway.actor.fetchUserMessages(App.getAccount())
                if (result is Result.Error) {
                    showAlert(result.exception); return@async
                }
                val messageList = result.get()

                loadVisibleMessages(PatronMessage.makeArray(messageList))
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

    private fun loadVisibleMessages(messageList: List<PatronMessage>) {
        items.clear()
        messageList.forEach {
            if (!it.isDeleted && it.isPatronVisible) {
                items.add(it)
            }
        }
    }

    private fun updateList() {
        adapter?.notifyDataSetChanged()
    }
}
