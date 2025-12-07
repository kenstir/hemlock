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

package net.kenstir.ui.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.logging.Log.TAG_EXTENSIONS
import net.kenstir.ui.App
import org.evergreen_ils.gateway.GatewayError
import net.kenstir.util.getCustomMessage
import androidx.core.net.toUri

fun Activity.showAlert(message: String, title: String? = "Error") {
    if (isFinishing) return
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
    val alertDialog = builder.create()
    alertDialog.show()
}

fun Activity.showAlert(ex: Exception) {
    Log.d(TAG_EXTENSIONS, "showAlert: ${ex.javaClass.simpleName}: ${ex.getCustomMessage()}", ex)
    if (ex is GatewayError && ex.isSessionExpired()) {
        showSessionExpiredAlert(ex)
    } else {
        showAlert(ex.getCustomMessage())
    }
}

fun Activity.showSessionExpiredAlert(ex: Exception) {
    if (isFinishing) return
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Error")
            .setMessage(ex.getCustomMessage())
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Login Again") { _, _ -> App.restartApp(this) }
    val alertDialog = builder.create()
    alertDialog.show()
}

/**
 * Enables edge-to-edge mode for the activity, allowing the content to extend into the system bars area.
 *
 * Call this before [Activity.setContentView].  After setContentView, call [ViewCompat.setOnApplyWindowInsetsListener]
 * to adjust the insets.  See [net.kenstir.ui.account.AuthenticatorActivity.onCreate] for an example.
 */
fun Activity.compatEnableEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
}

/** Launches a URL in the system browser.
 *
 * If no browser is installed, shows a Toast message.
 *
 * @param url The URL to launch.
 * @param requestId Optional request ID for startActivityForResult.  If null, uses startActivity.
 */
fun Activity.launchURL(url: String?, requestId: Int? = null) {
    if (url.isNullOrEmpty()) {
        Toast.makeText(this, R.string.msg_null_url, Toast.LENGTH_LONG).show()
        return
    }

    // Starting with Android 11 (API level 30), you should just catch ActivityNotFoundException;
    // calling resolveActivity requires permission.
    // https://developer.android.com/training/package-visibility/use-cases
    val uri = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        if (requestId != null) {
            startActivityForResult(intent, requestId)
        } else {
            startActivity(intent)
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.msg_no_browser_installed, Toast.LENGTH_LONG).show()
    }
}
