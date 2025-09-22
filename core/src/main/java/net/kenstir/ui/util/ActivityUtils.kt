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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import net.kenstir.hemlock.R

object ActivityUtils {
    @JvmStatic
    // TODO: Move to ActivityExtensions.kt once there are no Java usages.
    fun launchURL(activity: Activity, url: String?) {
        if (url.isNullOrEmpty()) {
            Toast.makeText(activity, R.string.msg_null_url, Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.msg_no_browser_installed, Toast.LENGTH_LONG).show()
        }
    }
}
