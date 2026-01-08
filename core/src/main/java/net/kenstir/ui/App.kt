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
import net.kenstir.data.service.ServiceConfig
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.util.CoilImageLoader.setImageLoader
import java.io.File

object App {
    private const val TAG = "App"

    // request/result codes for use with startActivityForResult
    const val REQUEST_MESSAGES: Int = 10002

    var account: Account = Account.noAccount
    var fcmNotificationToken: String? = null

    lateinit var behavior: AppBehavior
    lateinit var svc: ServiceConfig

    var library = Library("", "")
        set(value) {
            field = value
            svc.loaderService.setServiceUrl(value.url)
        }

    fun init(context: Context) {
        val isAndroidTest = context.resources.getBoolean(R.bool.is_android_test)
        Log.d(TAG, "[init] App.init isAndroidTest=$isAndroidTest")

        behavior = AppFactory.makeBehavior(context.resources)

        if (!this::svc.isInitialized) {
            svc = ServiceConfig()
        }
        configureServiceHttpClient(context)
    }

    private fun configureServiceHttpClient(context: Context) {
        val client = svc.loaderService.makeOkHttpClient(
            File(context.cacheDir, "okhttp")
        )
        setImageLoader(context, client)
    }
}
