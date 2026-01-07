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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import net.kenstir.hemlock.R
import net.kenstir.logging.Log

val Context.appVersionCode: String
    get() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            return pInfo.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("Log", "caught", e)
            return "0"
        }
    }

val Context.appInfo: String
    get() {
        var pInfo: PackageInfo? = null
        try {
            pInfo = packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("Log", "caught", e)
        }
        val appName: String = getString(R.string.ou_app_label)
        val versionName = pInfo?.versionName ?: "1.0.0.1"
        val verCode = pInfo?.versionCode ?: 0
        val version = "$appName $verCode ($versionName)"
        return version
    }
