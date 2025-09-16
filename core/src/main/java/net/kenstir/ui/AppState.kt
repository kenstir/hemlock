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

    // keys for prefs
    const val LIST_SORT_BY = "list_sort_by"
    const val LIST_SORT_DESCENDING = "list_sort_descending"
    const val HOLD_NOTIFY_BY_EMAIL = "notify_by_email"
    const val HOLD_NOTIFY_BY_PHONE = "notify_by_phone"
    const val HOLD_NOTIFY_BY_SMS = "notify_by_sms"
    const val HOLD_PHONE_NUMBER = "phone_number"
    const val HOLD_PICKUP_ORG_ID = "pickup_org_id"
    const val HOLD_SMS_CARRIER_ID = "sms_carrier_id"
    const val HOLD_SMS_NUMBER = "sms_number"
    const val LAUNCH_COUNT = "launch_count"
    const val LIBRARY_URL = "library_url"
    const val LIBRARY_NAME = "library_name"
    const val NIGHT_MODE = "night_mode"
    const val NOTIFICATIONS_DENY_COUNT = "notifications_deny_count"
    const val SEARCH_CLASS = "search_class"
    const val SEARCH_FORMAT = "search_format"
    const val SEARCH_OPTIONS_ARE_VISIBLE = "search_options_visible"
    const val SEARCH_ORG_SHORT_NAME = "search_org"

    // increment PREFS_VERSION every time you make a breaking change to the persistent pref storage
    private const val PREFS_VERSION = 2
    private const val VERSION = "version"
    private var initialized = false
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (initialized) return

        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        initialized = true

        // set default values unless already set
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
        val value = prefs.getString(key, defaultValue)
        Log.d(TAG, "[prefs] Got $key = $value")
        return value
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val value = prefs.getBoolean(key, defaultValue)
        Log.d(TAG, "[prefs] Got $key = $value")
        return value
    }

    @JvmStatic
    fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        val value = prefs.getInt(key, defaultValue)
        Log.d(TAG, "[prefs] Got $key = $value")
        return value
    }

    fun setString(key: String, value: String?) {
        Log.d(TAG, "[prefs] Set $key = $value")
        prefs.edit {
            putString(key, value)
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        Log.d(TAG, "[prefs] Set $key = $value")
        prefs.edit {
            putBoolean(key, value)
        }
    }

    @JvmStatic
    fun setInt(key: String, value: Int) {
        Log.d(TAG, "[prefs] Set $key = $value")
        prefs.edit {
            putInt(key, value)
        }
    }
}
