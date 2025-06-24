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

import android.util.Log

/**
 * Created by kenstir on 1/29/2017.
 */
class AndroidLogProvider: LogProvider {
    override fun v(tag: String?, msg: String) {
        Log.v(tag, msg)
    }

    override fun d(tag: String?, msg: String) {
        Log.d(tag, msg)
    }

    override fun d(tag: String?, msg: String, tr: Throwable?) {
        Log.d(tag, msg, tr)
    }

    companion object {
        fun create(): LogProvider? {
            val androidLoggingAvailable = try {
                // Android's Log class is present in unit tests, but throws a RuntimeException, so we can't simply
                // check for the presence of the android.util.Log class.
                Log.v(null, "Initializing AndroidLogProvider")
                true
            } catch (e: RuntimeException) {
//                e.message?.contains("not mocked") != true
                false
            }
            return if (androidLoggingAvailable) AndroidLogProvider() else null
        }
    }
}
