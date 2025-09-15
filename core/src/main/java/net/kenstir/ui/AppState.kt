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
package net.kenstir.ui

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import androidx.core.content.edit

/** App State that is persistent across invocations; stored as preferences.
 */
object AppState {
    private val TAG: String = AppState::class.java.simpleName

    const val LAUNCH_COUNT: String = "launch_count"
    const val LIBRARY_URL: String = "library_url"
    const val LIBRARY_NAME: String = "library_name"
    const val NIGHT_MODE: String = "night_mode"
    const val NOTIFICATIONS_DENY_COUNT: String = "notifications_deny_count"

    // increment PREFS_VERSION every time you make a change to the persistent pref storage
    private const val PREFS_VERSION = 2
    private const val VERSION = "version"
    private var initialized = false
    private lateinit var prefs: SharedPreferences

    fun init(callingContext: Context) {
        if (initialized) return

        val context = callingContext.applicationContext
        initialized = true

        // set default values unless already set
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var version = prefs.getInt(VERSION, 0)
        if (version < PREFS_VERSION) {
            prefs.edit(commit = true) {
                version = PREFS_VERSION
                putInt(VERSION, PREFS_VERSION)
                putString(LIBRARY_URL, context.getString(R.string.ou_library_url))
                putString(LIBRARY_NAME, context.getString(R.string.ou_library_name))
            }
        }
        Log.d(TAG, "version=$version")
        Log.d(TAG, "library_url=" + getString(LIBRARY_URL))
        Log.d(TAG, "library_name=" + getString(LIBRARY_NAME))
    }

    fun getString(key: String): String? {
        return getString(key, null)
    }

    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun setString(key: String, value: String?) {
        prefs.edit {
            putString(key, value)
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit {
            putBoolean(key, value)
        }
    }

    @JvmStatic
    fun setInt(key: String, value: Int) {
        prefs.edit {
            putInt(key, value)
        }
    }
}
