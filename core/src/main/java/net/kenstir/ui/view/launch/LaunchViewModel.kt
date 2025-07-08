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

package net.kenstir.ui.view.launch

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.ui.App
import net.kenstir.logging.Log
import net.kenstir.data.service.LoadStartupOptions
import net.kenstir.data.Result
import org.evergreen_ils.system.EgMessageMap
import net.kenstir.util.getCustomMessage
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "LaunchViewModel"

class LaunchViewModel : ViewModel() {

    private var errors = AtomicInteger(0)

    private val _status = MutableLiveData<String>()
    val status: LiveData<String>
        get() = _status

    private val _spinner = MutableLiveData<Boolean>()
    val spinner: LiveData<Boolean>
        get() = _spinner

    private val _serviceDataReady = MutableLiveData<Boolean>()
    val serviceDataReady: LiveData<Boolean>
        get() = _serviceDataReady

    private fun onLoadError(ex: Exception) {
        errors.incrementAndGet()
        Log.d(TAG, "ex:$ex")
        try {
            _status.value = ex.getCustomMessage()
        } catch (ex: java.lang.Exception) {
            _status.value = "Unexpected error"
        }
        _serviceDataReady.value = false
    }

    // This is where most global initialization happens.
    //
    // TODO: why did I use async inside async?
    fun loadServiceData(context: Context) {
        viewModelScope.async {
            val outerJob = viewModelScope.async {
                // update the UI
                _spinner.value = true
                _status.value = "Connecting to server"
                errors.set(0)

                // load the IDL etc.
                val options = LoadStartupOptions(net.kenstir.ui.App.getVersion(context), context.resources.getBoolean(R.bool.ou_hierarchical_org_tree))
                when (val result = net.kenstir.ui.App.getServiceConfig().loaderService.loadStartupPrerequisites(options)) {
                    is Result.Success -> {}
                    is Result.Error -> { onLoadError(result.exception) ; return@async }
                }

                // load custom messages from resources
                // TODO: move Evergreen-specific init to some evergreen-specific package
                EgMessageMap.init(context.resources)

                _status.value = "Connected"
                _serviceDataReady.value = true
            }
            try {
                outerJob.await()
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] caught in loadServiceData", ex)
                _status.value = ex.message
                _serviceDataReady.value = false
            } finally {
                _spinner.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
    }
}
