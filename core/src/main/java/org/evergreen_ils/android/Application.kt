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

package org.evergreen_ils.android

import org.evergreen_ils.utils.ui.AppState
import org.evergreen_ils.utils.ui.ThemeManager

class Application : androidx.multidex.MultiDexApplication() {
    private val TAG = javaClass.simpleName

    override fun onCreate() {
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        super.onCreate()

        AppState.init(this)
        val changed = ThemeManager.applyNightMode()
        Log.d(TAG, "applyNightMode returned $changed")
    }
}
