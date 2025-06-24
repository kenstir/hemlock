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
package net.kenstir.hemlock.logging

import android.annotation.SuppressLint

/** private logging class that allows substituting different behaviors
 */
object Log {
    // defining these statics here allows me to unit test code that logs / calls analytics
    // values from https://developer.android.com/reference/android/util/Log
    const val VERBOSE: Int = 2
    const val DEBUG: Int = 3
    const val WARN: Int = 5

    val provider: LogProvider by lazy { getLogProvider() }
    var level: Int = DEBUG

    var TAG_ASYNC: String = "async"
    var TAG_FCM: String = "fcm"
    var TAG_PERM: String = "perm"

    fun v(tag: String, msg: String) {
        if (level <= VERBOSE) provider.v(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (level <= DEBUG) provider.d(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, tr: Throwable?) {
        if (level <= DEBUG) provider.d(tag, msg, tr)
    }

    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun logElapsedTime(tag: String, startTime: Long, s: String): Long {
        val now = System.currentTimeMillis()
        provider.d(tag, String.format("%3dms: %s", now - startTime, s))
        return now
    }

    private fun getLogProvider(): LogProvider {
        return AndroidLogProvider.create() ?: StdoutLogProvider()
    }
}
