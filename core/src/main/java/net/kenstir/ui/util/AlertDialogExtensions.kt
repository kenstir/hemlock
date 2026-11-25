/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package net.kenstir.ui.util

import android.os.Build
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AlertDialog


/**
 * Set the message, where the message possibly contains HTML formatting.  Not all HTML tags are supported.
 * Call [AlertDialog.Builder.showAndMakeLinksClickable] to make any links clickable.
 */
fun AlertDialog.Builder.setMessageFromHtml(message: String): AlertDialog.Builder {
    // build the alert dialog with simple HTML rendering of the message
    val spannedMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(message)
    }
    return this.setMessage(spannedMessage)
}

/**
 * Show an alert dialog with a message possibly containing HTML formatting.  Any links in the message will be clickable.
 */
fun AlertDialog.Builder.showAndMakeLinksClickable() {
    val dialog = this.create()
    dialog.show()

    // set the movementMethod on the TextView to make any links clickable
    val messageView: TextView? = dialog.findViewById(android.R.id.message)
    messageView?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
}
