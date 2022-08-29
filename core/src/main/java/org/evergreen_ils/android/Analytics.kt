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
package org.evergreen_ils.android

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.evergreen_ils.BuildConfig
import org.evergreen_ils.R
import org.evergreen_ils.data.Organization
import org.evergreen_ils.data.Result
import org.evergreen_ils.utils.getCustomMessage
import java.text.SimpleDateFormat
import java.util.*

/** Utils that wrap Crashlytics (and now Analytics)
 */
object Analytics {
    object Event {
        const val ACCOUNT_ADD = "account_add"
        const val ACCOUNT_LOGOUT = "account_logout"
        const val ACCOUNT_SWITCH = "account_switch"
        const val BOOKBAG_ADD_ITEM = "bookbag_add_item"
        const val BOOKBAG_DELETE_ITEM = "bookbag_delete_item"
        const val BOOKBAG_LOAD = "bookbag_load"
        const val BOOKBAGS_CREATE_LIST = "bookbags_create_list"
        const val BOOKBAGS_DELETE_LIST = "bookbags_delete_list"
        const val BOOKBAGS_LOAD = "bookbags_load"
        const val FEEDBACK_OPEN = "feedback_open"
        const val HOLD_CANCEL_HOLD = "hold_cancel"
        const val HOLD_PLACE_HOLD = "hold_place"
        const val HOLD_UPDATE_HOLD = "hold_update"
        const val LOGIN = FirebaseAnalytics.Event.LOGIN
        const val MESSAGES_OPEN = "messages_open"
        const val SEARCH = FirebaseAnalytics.Event.SEARCH
        const val VIEW_ITEM_DETAILS = FirebaseAnalytics.Event.VIEW_ITEM
    }

    object Param {
        const val HOLD_EXPIRES_KEY = "hold_expires" // bool
        const val HOLD_NOTIFY = "hold_notify"
        const val HOLD_PICKUP_KEY = "hold_pickup" // { home | other }
        const val HOLD_REACTIVATE_KEY = "hold_reactivate" // bool
        const val HOLD_SUSPEND_KEY = "hold_suspend" // bool
        const val LOGIN_TYPE = "login_type" // { barcode | username }
        const val NUM_ITEMS = "num_items"
        const val NUM_RESULTS = "num_results"
        const val RESULT = "result" // { ok | error_message }
        const val SEARCH_CLASS = "search_class"
        const val SEARCH_FORMAT = "search_format"
        const val SEARCH_ORG_KEY = "search_org" // { home | other }
        const val SEARCH_TERM = FirebaseAnalytics.Param.SEARCH_TERM
    }

    // these need to be registered in GA
    object UserProperty {
        const val HOME_ORG = "home_org"
        const val PARENT_ORG = "parent_org"
    }

    object Value {
        const val OK = "ok"
    }

    private val TAG = Analytics::class.java.simpleName
    private var initialized = false
    private var analytics = false
    private var runningInTestLab = false
    private var mAnalytics: FirebaseAnalytics? = null
    private const val mQueueSize = 256
    private val mEntries = ArrayDeque<String>(mQueueSize)
    private val mTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val mRedactedResponseRegex = Regex("""
        ("__c":"aum?"|"__c":"aou"|"authtoken":)
    """.trimIndent())

    @JvmStatic
    fun initialize(context: Context) {
        if (initialized) return

        val setting: String? = Settings.System.getString(context.contentResolver, "firebase.test.lab")
        runningInTestLab = (setting == "true")

        if (context.resources.getBoolean(R.bool.ou_enable_analytics) && !runningInTestLab) {
            analytics = true
            if (mAnalytics == null)
                mAnalytics = FirebaseAnalytics.getInstance(context)
        }

        initialized = true
    }

    @JvmStatic
    fun setString(key: String?, value: String?) {
//        if (analytics) Crashlytics.setString(key, val);
    }

    @JvmStatic
    fun redactedString(value: String?): String {
        if (value == null) return "(null)"
        return if (value.isEmpty()) "(empty)" else "***"
    }

    @JvmStatic
    fun log(tag: String, msg: String) {
        Log.d(tag, msg)
        if (analytics) FirebaseCrashlytics.getInstance().log(msg)
    }

    @JvmStatic
    fun logException(tag: String?, e: Throwable) {
        Log.d(tag, "caught", e)
        addToLogBuffer("err:  ${e.stackTraceToString()}")
        if (analytics) FirebaseCrashlytics.getInstance().recordException(e)
    }

    @JvmStatic
    fun logException(e: Throwable) {
        logException(TAG, e)
    }

    fun setUserProperties(b: Bundle?) {
        if (b == null) return
        if (analytics) {
            for (name in b.keySet()) {
                mAnalytics?.setUserProperty(name, b.getString(name))
                Log.d(TAG, "setUserProperty $name=${b.getString(name)}")
            }
        }
    }

    @JvmStatic
    fun logEvent(event: String, b: Bundle?) {
        if (event.length > 40) {
            if (BuildConfig.DEBUG) throw AssertionError("Event name is too long")
            return
        }
        if (analytics) mAnalytics?.logEvent(event, b)
    }

    @JvmStatic
    fun logEvent(event: String) {
        logEvent(event, null)
    }

    @JvmStatic
    fun logEvent(event: String, name: String?, value: String?) {
        val b = Bundle()
        b.putString(name, value)
        logEvent(event, b)
    }

    // Add request to the logBuffer to be available in an error report
    @JvmStatic
    fun logRequest(tag: String, url: String) {
        addToLogBuffer("$tag send: $url")
    }

    // Add response to the logBuffer to be available in an error report
    @JvmStatic
    fun logResponse(tag: String, url: String, cached: Boolean, data: String) {
        // trim or redact certain responses
        if (data.startsWith("<IDL ")) {
            addToLogBuffer("$tag recv: <IDL>")
        } else if (mRedactedResponseRegex.containsMatchIn(data)) {
//            addToLogBuffer("$tag recv: ***")
            addToLogBuffer("$tag recv: *** $data")
        } else {
            addToLogBuffer("$tag recv: $data")
        }
    }

    private fun addToLogBuffer(msg: String) {
        val sb = java.lang.StringBuilder()
        val date = mTimeFormat.format(System.currentTimeMillis())
        sb.append(date).append(' ').append(msg)
        mEntries.push(sb.toString())
        //Log.d(TAG, "[LOGBUF] ${sb.toString()}")

        while (mEntries.size > mQueueSize) {
            mEntries.pop()
        }
    }

    @JvmStatic
    fun getLogBuffer(): String {
        val sb = StringBuilder(mQueueSize * 120)
        val it = mEntries.descendingIterator()
        while (it.hasNext()) {
            sb.append(it.next()).append('\n')
        }
        return sb.toString()
    }

    fun orgDimensionKey(selectedOrg: Organization?, defaultOrg: Organization?, homeOrg: Organization?): String {
        return when {
            selectedOrg == null || defaultOrg == null || homeOrg == null -> "null"
            selectedOrg.id == defaultOrg.id -> "default"
            selectedOrg.id == homeOrg.id -> "home"
            selectedOrg.isConsortium -> selectedOrg.shortname
            else -> "other"
        }
    }

    fun loginTypeKey(username: String, barcode: String?): String {
        return when {
            username == barcode -> "barcode"
            else -> "username"
        }
    }

    fun resultValue(result: Result<Any?>): String {
        return when (result) {
            is Result.Error -> result.exception.getCustomMessage()
            is Result.Success -> Value.OK
        }
    }
}
