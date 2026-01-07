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

package net.kenstir.ui

import android.content.Context
import net.kenstir.data.model.Account
import net.kenstir.data.model.Library
import net.kenstir.hemlock.R
import net.kenstir.logging.Log

object Appx {
    private const val TAG = "Appx"

    lateinit var account: Account
    lateinit var behavior: AppBehavior
    lateinit var library: Library

    fun init(context: Context) {
        val isAndroidTest = context.getResources().getBoolean(R.bool.is_android_test)
        Log.d(TAG, "[init] App.init isAndroidTest=$isAndroidTest")
        behavior = AppFactory.makeBehavior(context.resources)
    }
}
