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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.kenstir.logging.Log
import net.kenstir.ui.util.ThemeManager
import java.io.File

class Application : androidx.multidex.MultiDexApplication() {

    // Define a scope that lives as long as the application
    // Use SupervisorJob so if one child coroutine fails, others aren't cancelled
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        Log.d(TAG, "[init] Application.onCreate kotlin ${KotlinVersion.CURRENT}")
        super.onCreate()

        AppState.init(this)
        val changed = ThemeManager.applyNightMode()
        Log.d(TAG, "[init] applyNightMode returned $changed")

        App.init(this)
        deleteLegacyCacheDirectory()

        // useful for debugging TransactionTooLargeException
        //TooLargeTool.startLogging(this)
    }

    private fun deleteLegacyCacheDirectory() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Delete the pre-4.0 cache directory if it exists
                val volleyCacheDir = File(applicationContext.cacheDir, "volley")
                val ok = volleyCacheDir.deleteRecursively()
                Log.d(TAG, "[init] Deleted volley cache directory: $ok")
            } catch (e: Exception) {
                Log.d(TAG, "[init] Failed to delete volley cache directory", e)
            }
        }
    }

    companion object {
        private const val TAG = "Application"
    }
}
