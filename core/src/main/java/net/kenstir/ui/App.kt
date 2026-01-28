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
import android.content.res.Resources
import net.kenstir.data.model.Account
import net.kenstir.data.model.Library
import net.kenstir.data.service.ServiceConfig
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.util.CoilImageLoader.initImageLoader
import net.kenstir.util.Analytics.logException
import java.io.File

object App {
    private const val TAG = "App"

    // request/result codes for use with startActivityForResult
    const val REQUEST_MESSAGES: Int = 10002

    var account: Account = Account.noAccount
    var fcmNotificationToken: String? = null

    lateinit var behavior: AppBehavior
    lateinit var factory: AppFactory
    lateinit var svc: ServiceConfig

    var library = Library("", "")
        set(value) {
            field = value
            svc.loader.serviceUrl = value.url
        }

    fun init(context: Context) {
        val isAndroidTest = context.resources.getBoolean(R.bool.is_android_test)
        Log.d(TAG, "[init] App.init isAndroidTest=$isAndroidTest")

        factory = makeFactory(context.resources)
        behavior = factory.makeBehavior()
        svc = factory.makeServiceConfig(isAndroidTest)

        val okHttpClient = svc.loader.initHttpClient(
            File(context.cacheDir, "okhttp")
        )
        initImageLoader(context, okHttpClient)
    }

    fun makeFactory(resources: Resources): AppFactory {
        val clazzName = resources.getString(R.string.app_factory_provider)
        if (clazzName.isEmpty()) {
            throw IllegalStateException("No AppFactory class configured in resources")
        } else {
            Log.d(TAG, "[init] Constructing $clazzName")
            try {
                val clazz = Class.forName(clazzName)
                return clazz.newInstance() as AppFactory
            } catch (e: Exception) {
                logException(e)
                throw IllegalStateException("Could not instantiate AppFactory: $clazzName", e)
            }
        }
    }
}
