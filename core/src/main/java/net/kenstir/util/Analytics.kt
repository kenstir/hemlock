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
package net.kenstir.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import net.kenstir.hemlock.BuildConfig
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.data.model.Organization
import net.kenstir.data.Result
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

/** Utils that wrap Crashlytics (and now FirebaseAnalytics)
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
        //const val LOGIN = FirebaseAnalytics.Event.LOGIN // avoid builtin name so my custom dimension works
        const val LOGIN = "login_v2"
        const val MESSAGES_OPEN = "messages_open"
        const val SCAN = "barcode_scan"
        const val SEARCH = FirebaseAnalytics.Event.SEARCH
        const val SEARCH_ADV_SEARCH = "search_advanced_search"
        const val SEARCH_ADV_CANCEL = "search_advanced_cancel"
        const val VIEW_ITEM_DETAILS = FirebaseAnalytics.Event.VIEW_ITEM
    }

    object Param {
        // these need to be registered in FA as Custom Dimensions w/ scope=Event
        const val HOLD_EXPIRES_KEY = "hold_expires" // bool
        const val HOLD_NOTIFY = "hold_notify"
        const val HOLD_PICKUP_KEY = "hold_pickup" // { home | other }
        const val HOLD_REACTIVATE_KEY = "hold_reactivate" // bool
        const val HOLD_SUSPEND_KEY = "hold_suspend" // bool
        const val LOGIN_TYPE = "login_type" // { barcode | username }
        const val RESULT = "result" // { ok | error_message }
        const val SEARCH_CLASS = "search_class"
        const val SEARCH_FORMAT = "search_format"
        const val SEARCH_ORG_KEY = "search_org" // { home | other }
        //const val SEARCH_TERM = FirebaseAnalytics.Param.SEARCH_TERM omitted for privacy

        // these need to be registered in FA as Custom Metrics
        const val NUM_ACCOUNTS = "num_accounts"
        const val NUM_ITEMS = "num_items"
        const val NUM_RESULTS = "num_results"
        const val SEARCH_TERM_UNIQ_WORDS = "search_term_uniq_words"
        const val SEARCH_TERM_AVG_WORD_LEN_X10 = "search_term_avg_word_len_x10"

        // boolean params do not need to be registered
        const val MULTIPLE_ACCOUNTS = "multiple_accounts"
    }

    object UserProperty {
        // these need to be registered in FA as Custom Dimensions w/ scope=User
        const val HOME_ORG = "user_home_org"
        const val PARENT_ORG = "user_parent_org"
    }

    object Value {
        const val OK = "ok"
    }

    private const val TAG = "Analytics"
    private var initialized = false
    private var analytics = false
    private var runningInTestLab = false
    private var mAnalytics: FirebaseAnalytics? = null
    private const val LOG_BUFFER_SIZE = 64
    const val MAX_DATA_SHOWN = 512 // max length of data shown in logResponseX
    private val mEntries = ArrayDeque<String>(LOG_BUFFER_SIZE)
    private val mTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val mRedactedResponseRegex = Regex("""
        ("__c":"aum?"|"authtoken":)
    """.trimIndent())

    @JvmStatic
    fun initialize(context: Context) {
        if (initialized) return

        val setting: String? = Settings.System.getString(context.contentResolver, "firebase.test.lab")
        runningInTestLab = (setting == "true")

        if (wantAnalytics(context)) {
            analytics = true
            if (mAnalytics == null)
                mAnalytics = FirebaseAnalytics.getInstance(context)
        }

        initialized = true
    }

    private fun wantAnalytics(context: Context): Boolean {
        // To enable debug mode on a device:
        //   adb shell setprop debug.firebase.analytics.app PACKAGE_NAME
        // Log verbosely:
        //   adb shell setprop log.tag.FA VERBOSE
        // Disable:
        //   adb shell setprop debug.firebase.analytics.app .none.
        //   adb shell setprop log.tag.FA SILENT
        // See also: https://firebase.google.com/docs/analytics/debugview#android

        // Usually I do not want to include events while I am running in the debugger
        val includeEventsForDebugBuilds = false
        return context.resources.getBoolean(R.bool.ou_enable_analytics) && !runningInTestLab && (!isDebuggable(context) || includeEventsForDebugBuilds)
    }

    fun isDebuggable(context: Context): Boolean {
        return 0 != ApplicationInfo.FLAG_DEBUGGABLE.let { context.applicationInfo.flags = context.applicationInfo.flags and it; context.applicationInfo.flags }
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
    fun logException(tag: String, e: Throwable) {
        Log.d(tag, "caught", e)
        addToLogBuffer("err:  ${e.stackTraceToString()}")
        if (analytics) FirebaseCrashlytics.getInstance().recordException(e)
    }

    @JvmStatic
    fun logException(e: Throwable) {
        logException(TAG, e)
    }

    private fun setUserProperties(b: Bundle?) {
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
    fun logRequest(debugTag: String, httpMethod: String, url: String) {
        val tag8 = debugTag.padStart(8)
        val method = httpMethod.padEnd(4)
        val logMsg = "[net] $tag8 $method  $url"
        Log.d(TAG, logMsg)
        addToLogBuffer(logMsg)
    }

    fun logResponseX(debugTag: String, url: String, cached: Boolean, data: String, elapsed: Long? = null) {
        val tag8 = debugTag.padStart(8)
        val badge = if (cached) "*" else " "
        val prefix =
            if (elapsed != null) {
                "[net] $tag8 recv$badge %5d ms".format(elapsed)
            } else {
                "[net] $tag8 recv$badge"
            }
        // trim or redact certain responses
        val logMsg =
            if (data.startsWith("<IDL ")) {
                "$prefix <IDL>"
            } else if (mRedactedResponseRegex.containsMatchIn(data)) {
                "$prefix ***"
            } else {
                "$prefix ${data.take(MAX_DATA_SHOWN)}"
            }
        Log.d(TAG, logMsg)
        addToLogBuffer(logMsg)
    }

    @Synchronized
    private fun addToLogBuffer(msg: String) {
        val sb = java.lang.StringBuilder()
        val date = mTimeFormat.format(System.currentTimeMillis())
        sb.append(date).append(' ').append(msg)
        mEntries.push(sb.toString())
        //Log.d(TAG, "[LOG] ${sb.toString()}")

        while (mEntries.size > LOG_BUFFER_SIZE) {
            mEntries.pop()
        }
    }

    @JvmStatic
    @Synchronized
    fun getLogBuffer(): String {
        val sb = StringBuilder(LOG_BUFFER_SIZE * 120)
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

    private fun loginTypeKey(username: String, barcode: String?): String {
        return when {
            username == barcode -> "barcode"
            else -> "username"
        }
    }

    fun searchTextStats(searchText: String): Bundle {
        // Count unique words in searchText
        val words = searchText.lowercase().split("\\s+".toRegex()).toTypedArray()
        val wordSet = HashSet<String>()
        wordSet.addAll(words)

        // Calculate average word length
        val totalLen = wordSet.fold(0) { acc, word -> acc + word.length }
        val avgLen = if (totalLen > 0) totalLen / wordSet.size else 0

        // round avgLen * 10 to nearest integer
        val avgLenInt = round(10.0 * avgLen.toDouble()).toInt()

        return bundleOf(
            Param.SEARCH_TERM_UNIQ_WORDS to wordSet.size,
            Param.SEARCH_TERM_AVG_WORD_LEN_X10 to avgLenInt
        )
    }

    // We call this event "login", but it happens after auth and after fleshing the user.
    // NB: "session_start" seems more appropriate but that is a predefined automatic event.
    @JvmStatic
    fun logSuccessfulLogin(username: String, barcode: String?, homeOrg: String?, parentOrg: String, numAccounts: Int) {
        setUserProperties(bundleOf(
            UserProperty.HOME_ORG to homeOrg,
            UserProperty.PARENT_ORG to parentOrg
        ))
        logEvent(
            Event.LOGIN, bundleOf(
            Param.RESULT to Value.OK,
            Param.LOGIN_TYPE to loginTypeKey(username, barcode),
            Param.NUM_ACCOUNTS to numAccounts,
            Param.MULTIPLE_ACCOUNTS to (numAccounts > 1)
        ))
    }

    @JvmStatic
    fun logFailedLogin(ex: Exception) {
        val c = ex.javaClass.simpleName
        val m = ex.cause?.localizedMessage ?: ex.localizedMessage ?: c
        logEvent(
            Event.LOGIN, bundleOf(
            Param.RESULT to m
        ))
    }

    fun resultValue(result: Result<Any?>): String {
        return when (result) {
            is Result.Error -> result.exception.getCustomMessage()
            is Result.Success -> Value.OK
        }
    }
}
