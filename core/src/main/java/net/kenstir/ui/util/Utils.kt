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
package net.kenstir.ui.util

import android.os.Bundle
import android.os.Parcel
import net.kenstir.logging.Log

/**
 * Utility functions that depend on the Android platform, i.e. must be tested on an emulator.
 */
object Utils {
    private const val TAG = "Utils"

    @JvmStatic
    fun logBundleSize(source: String, bundle: Bundle?) {
        val parcel = Parcel.obtain()
        try {
            bundle?.writeToParcel(parcel, 0)
            val size = parcel.dataSize()
            Log.d(TAG, "[bundle] size = $size bytes ($source)")
        } finally {
            parcel.recycle()
        }
    }

}
