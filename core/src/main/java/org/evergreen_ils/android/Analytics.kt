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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.evergreen_ils.BuildConfig
import org.evergreen_ils.R
import org.opensrf.util.GatewayResult
import org.opensrf.util.OSRFObject

/** Utils that wrap Crashlytics (and now Analytics)
 */
object Analytics {
    object Event {
        const val ACCOUNT_ADD = "account_add"
        const val ACCOUNT_LOGOUT = "account_logout"
        const val ACCOUNT_SWITCH = "account_switch"
        const val FEEDBACK_OPEN = "feedback_open"
        const val HOLD_PLACEHOLD = "hold_place"
        const val MESSAGES_OPEN = "messages_open"

        const val LOGIN = FirebaseAnalytics.Event.LOGIN
        const val SEARCH = FirebaseAnalytics.Event.SEARCH
        const val VIEW_ITEM_DETAILS = FirebaseAnalytics.Event.VIEW_ITEM
    }

    object Param {
        const val ERROR_MESSAGE = "error_message"
        const val NUM_RESULTS = "num_results"
        const val SEARCH_CLASS = "search_class"
        const val SEARCH_FORMAT = "search_format"
        const val SEARCH_TERM = FirebaseAnalytics.Param.SEARCH_TERM
        const val SUCCEEDED = "succeeded"
    }

    private val TAG = Analytics::class.java.simpleName
    private const val MAX_PARAMS = 5
    private var mLastAuthToken: String? = null
    private var analytics = false
    private var mAnalytics: FirebaseAnalytics? = null

    @JvmStatic
    fun initialize(context: Context) {
        if (context.resources.getBoolean(R.bool.ou_enable_analytics)) {
            analytics = true
            if (mAnalytics == null)
                mAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    @JvmStatic
    fun setString(key: String?, value: String?) {
//        if (analytics) Crashlytics.setString(key, val);
    }

    @JvmStatic
    fun redactedString(value: String?): String {
        if (value == null) return "(null)"
        return if (value.length == 0) "(empty)" else "***"
    }

    @JvmStatic
    fun log(tag: String, msg: String) {
        Log.d(tag, msg)
        if (analytics) FirebaseCrashlytics.getInstance().log(msg)
    }

//    private fun redactResponse(o: OSRFObject, netClass: String): String {
//        return if (netClass == "au" || netClass == "aou" /*orgTree*/) {
//            "***"
//        } else {
//            o.toString()
//        }
//    }

    @JvmStatic
    fun logException(tag: String?, e: Throwable) {
        Log.d(tag, "caught", e)
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
            }
        }
    }

    @JvmStatic
    fun logEvent(event: String, b: Bundle?) {
        Log.d(TAG, "kcxxx: event.length:" + event.length + " name:" + event + " b:" + b)
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
}
