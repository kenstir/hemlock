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
import net.kenstir.logging.Log

fun Bundle.dumpContents(tag: String, msg: String) {
    val keys = keySet()
    Log.d("Misc", "$tag $msg Bundle contents:")
    for (key in keys) {
        Log.d("Misc", "$tag  $key: ${this.get(key)}")
    }
}
