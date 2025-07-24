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

package net.kenstir.apps.acorn

import android.app.Activity
import androidx.annotation.Keep
import net.kenstir.logging.Log
import net.kenstir.ui.view.MenuProvider

@Keep
class AcornMenuProvider : MenuProvider() {
    private val TAG = javaClass.simpleName

    override fun onCreate(activity: Activity?) {
        return;
    }

    override fun onItemSelected(activity: Activity?, id: Int, via: String?): Boolean {
        when (id) {
            0 /*R.id.main_my_account_button*/ -> {
                Log.d(TAG, "stop here")
            }
            else -> {
                Log.d(TAG, "stop here")
                return false
            }
        }
        return true
    }
}
