/*
 * Copyright (c) 2020 Kenneth H. Cox
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.utils.ui

import android.app.Activity
import android.app.AlertDialog
import org.evergreen_ils.android.App
import org.evergreen_ils.net.GatewayError
import org.evergreen_ils.system.Log
import org.evergreen_ils.utils.getCustomMessage

fun Activity.showAlert(ex: Exception) {
    if (ex is GatewayError && ex.isSessionExpired()) {
        showSessionExpiredAlert(ex)
    } else {
        showAlert(ex.getCustomMessage())
    }
}

fun Activity.showAlert(errorMessage: String) {
    if (isFinishing) return
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
    val alertDialog = builder.create()
    alertDialog.show()
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
                App.restartApp(this)
            }
    val alertDialog = builder.create()
    alertDialog.show()
}
