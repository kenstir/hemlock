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
import android.view.MenuItem
import android.widget.TextView
import org.evergreen_ils.R
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.PatronMessage
import org.evergreen_ils.utils.ui.ActionBarUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.w3c.dom.Text

class MessageDetailsActivity : BaseActivity() {
    val TAG = javaClass.simpleName

    private lateinit var message: PatronMessage
    private var title: TextView? = null
    private var body: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_message_details)
        ActionBarUtils.initActionBarForActivity(this)

        message = intent.getSerializableExtra("patronMessage") as PatronMessage

        val title = findViewById<TextView>(R.id.message_title)
        val body = findViewById<TextView>(R.id.message_body)

        title.text = message.title
        body.text = message.message
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}