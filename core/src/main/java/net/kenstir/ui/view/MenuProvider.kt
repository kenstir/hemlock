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
package net.kenstir.ui.view

import android.app.Activity
import net.kenstir.util.Analytics

/** Interface to get extra buttons provided by the main main of a custom app.
 * Concrete implementation is provided by the app
 * and the name of that class is specified in R.string.ou_main_menu_provider .
 */
abstract class MenuProvider {
    abstract fun onCreate(activity: Activity)
    abstract fun onItemSelected(activity: Activity, id: Int, via: String): Boolean

    companion object {
        fun create(clazzName: String): MenuProvider? {
            if (clazzName.isEmpty()) {
                return null
            }
            try {
                val clazz = Class.forName(clazzName)
                return clazz.newInstance() as MenuProvider
            } catch (e: Exception) {
                Analytics.logException(e)
                return null
            }
        }
    }
}
