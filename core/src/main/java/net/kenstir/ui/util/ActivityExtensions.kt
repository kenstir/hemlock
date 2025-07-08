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
import net.kenstir.logging.Log
import org.evergreen_ils.gateway.GatewayError
import net.kenstir.util.getCustomMessage

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
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                Log.d("sessionexpired", "cancel")
            }
            .setPositiveButton("Login Again") { _, _ ->
                Log.d("sessionexpired", "relog")
                net.kenstir.ui.App.restartApp(this)
            }
    val alertDialog = builder.create()
    alertDialog.show()
}
