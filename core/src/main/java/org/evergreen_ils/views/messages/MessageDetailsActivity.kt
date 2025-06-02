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
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.data.PatronMessage
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.ui.ActionBarUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.showAlert
import org.evergreen_ils.views.messages.MessagesActivity.Companion.RESULT_MESSAGE_UPDATED
import java.text.DateFormat

class MessageDetailsActivity : BaseActivity() {
    val TAG = javaClass.simpleName

    private lateinit var message: PatronMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_message_details)
        ActionBarUtils.initActionBarForActivity(this)

        message = intent.getSerializableExtra("patronMessage") as PatronMessage

        val title = findViewById<TextView>(R.id.message_title)
        val date = findViewById<TextView>(R.id.message_date)
        val body = findViewById<TextView>(R.id.message_body)

        title.text = message.title
        date.text = if (message.createDate != null) DateFormat.getDateInstance().format(message.createDate) else ""
        body.text = message.message
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        markMessageRead()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_message_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_message_mark_unread -> markMessageUnreadAndFinish()
            R.id.action_message_delete -> markMessageDeletedAndFinish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun markMessageDeletedAndFinish() {
        scope.async {
            val result = Gateway.actor.markMessageDeleted(App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
            finish()
        }
    }

    private fun markMessageRead() {
        scope.async {
            val result = Gateway.actor.markMessageRead(App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
        }
    }

    private fun markMessageUnreadAndFinish() {
        scope.async {
            val result = Gateway.actor.markMessageUnread(App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
            finish()
        }
    }
}
