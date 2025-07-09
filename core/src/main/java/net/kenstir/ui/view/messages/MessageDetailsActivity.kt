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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.ui.Key
import net.kenstir.ui.util.showAlert
import net.kenstir.data.Result
import net.kenstir.ui.App
import org.evergreen_ils.data.model.EvergreenPatronMessage
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.ActionBarUtils
import net.kenstir.ui.view.messages.MessagesActivity.Companion.RESULT_MESSAGE_UPDATED
import java.text.DateFormat

class MessageDetailsActivity : BaseActivity() {
    val TAG = javaClass.simpleName

    private lateinit var message: EvergreenPatronMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_message_details)
        ActionBarUtils.initActionBarForActivity(this)

        message = intent.getSerializableExtra(Key.PATRON_MESSAGE) as EvergreenPatronMessage

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
            val result = App.getServiceConfig().userService.markMessageDeleted(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
            finish()
        }
    }

    private fun markMessageRead() {
        scope.async {
            val result = App.getServiceConfig().userService.markMessageRead(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
        }
    }

    private fun markMessageUnreadAndFinish() {
        scope.async {
            val result = App.getServiceConfig().userService.markMessageUnread(
                App.getAccount(), message.id)
            if (result is Result.Error) {
                showAlert(result.exception); return@async
            }
            setResult(RESULT_MESSAGE_UPDATED)
            finish()
        }
    }
}
