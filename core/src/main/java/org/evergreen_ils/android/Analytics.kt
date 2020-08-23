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
    fun initialize(context: Context?) {
        if (mAnalytics == null) mAnalytics = FirebaseAnalytics.getInstance(context!!)
        analytics = true
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

    fun log(msg: String) {
        log(TAG, msg)
    }

//    fun logRequest(service: String?, method: Method, authToken: String?) {
//        mLastAuthToken = authToken
//        logRequest(service, method)
//    }

//    fun logRequest(service: String?, method: String?, params: List<Any>) {
//        if (!analytics) return
//        FirebaseCrashlytics.getInstance().setCustomKey("svc", service!!)
//        FirebaseCrashlytics.getInstance().setCustomKey("m", method!!)
//        val logParams: MutableList<String> = ArrayList()
//        var i: Int = 0
//        while (i < params.size) {
//            val key = "p$i"
//            var value = "" + params[i]
//            if (value.length > 0 && TextUtils.equals(value, mLastAuthToken)) value = "***" //redacted
//            logParams.add(value)
//            if (i < MAX_PARAMS) FirebaseCrashlytics.getInstance().setCustomKey(key, value)
//            i++
//        }
//        while (i < MAX_PARAMS) {
//            val key = "p$i"
//            FirebaseCrashlytics.getInstance().setCustomKey(key, null)
//            i++
//        }
//    }

//    fun logRequest(service: String?, method: Method) {
//        logRequest(service, method.name, method.params)
//    }

//    fun buildGatewayUrl(service: String?, method: String?, params: Array<Any?>): String {
//        logVolleyRequest(service, method, params)
//        return buildUrl(service!!, method!!, params)
//    }

//    fun logVolleyRequest(service: String?, method: String?, params: Array<Any?>) {
//        val p = Arrays.asList<Any>(*params)
//        logRequest(service, method, p)
//    }

    private fun redactResponse(o: OSRFObject, netClass: String): String {
        return if (netClass == "au" || netClass == "aou" /*orgTree*/) {
            "***"
        } else {
            o.toString()
        }
    }

    fun logResponse(resp: Any?) {
        if (!analytics) return
        try {
            if (resp is OSRFObject) {
                val o = resp
                val netClass = o.registry.netClass
                val s = redactResponse(o, netClass)
                //                Crashlytics.log(Log.DEBUG, TAG, "resp [" + netClass + "]: " + s);
                return
            }
        } catch (e: Exception) {
//            Crashlytics.log(Log.DEBUG, TAG, "exception parsing resp: " + e.getMessage());
        }
        //        Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp);
    }

    fun logResponse(resp: GatewayResult?) {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, "resp: " + resp.payload);
    }

    fun logRedactedResponse() {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, "resp: ***");
    }

    fun logVolleyResponse(method: String?) {
//        if (analytics) Crashlytics.log(Log.DEBUG, TAG, method + " ok");
    }

    fun logErrorResponse(resp: String?) {
//        if (analytics) Crashlytics.log(Log.WARN, TAG, "err_resp: " + resp);
    }

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

    fun logEvent(event: String, name: String?, value: Boolean) {
        val b = Bundle()
        b.putBoolean(name, value)
        logEvent(event, b)
    }

    fun logEvent(event: String, name: String?, value: Int) {
        val b = Bundle()
        b.putLong(name, value.toLong())
        logEvent(event, b)
    }

    fun logEvent(event: String, name: String?, value: String?, n2: String?, v2: String?) {
        val b = Bundle()
        b.putString(name, value)
        b.putString(n2, v2)
        logEvent(event, b)
    }

    fun logEvent(event: String, name: String?, value: Int, n2: String?, v2: Boolean) {
        val b = Bundle()
        b.putLong(name, value.toLong())
        b.putBoolean(n2, v2)
        logEvent(event, b)
    }
}
