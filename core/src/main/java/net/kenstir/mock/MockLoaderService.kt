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

package net.kenstir.mock

import android.content.res.Resources
import net.kenstir.data.Result
import net.kenstir.data.service.LoadStartupOptions
import net.kenstir.data.service.LoaderService
import okhttp3.OkHttpClient
import java.io.File

object MockLoaderService : LoaderService {
    const val DEFAULT_TIMEOUT_MS = 2_000

    lateinit var okHttpClient: OkHttpClient

    override fun makeOkHttpClient(cacheDir: File): OkHttpClient {
        okHttpClient = OkHttpClient.Builder()
            .callTimeout(DEFAULT_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        return okHttpClient
    }

    override fun setServiceUrl(url: String) {
        TODO("Not yet implemented")
    }

    override suspend fun loadStartupPrerequisites(serviceOptions: LoadStartupOptions, resources: Resources): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun loadPlaceHoldPrerequisites(): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPublicIpAddress(): Result<String> {
        TODO("Not yet implemented")
    }
}
