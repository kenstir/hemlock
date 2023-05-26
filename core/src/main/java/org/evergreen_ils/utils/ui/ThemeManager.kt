/*
 * Copyright (c) 2019 Kenneth H. Cox
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

import androidx.appcompat.app.AppCompatDelegate
import org.evergreen_ils.android.Log

object ThemeManager {
    private val TAG = ThemeManager::class.java.simpleName

    fun applyNightMode(): Boolean {
        val nightMode = AppState.getInt(AppState.NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)
        Log.d(TAG, "applyNightMode:$nightMode")
        return applyNightMode(nightMode)
    }

    fun saveAndApplyNightMode(nightMode: Int): Boolean {
        AppState.setInt(AppState.NIGHT_MODE, nightMode)
        Log.d(TAG, "saveAndApplyNightMode:$nightMode")
        return applyNightMode(nightMode)
    }

    private fun applyNightMode(nightMode: Int): Boolean {
        val oldNightMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(nightMode)
        return oldNightMode != nightMode
    }
}