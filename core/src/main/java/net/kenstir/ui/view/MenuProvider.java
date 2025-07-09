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

package net.kenstir.ui.view;

import android.app.Activity;
import android.text.TextUtils;

import net.kenstir.util.Analytics;

/** Interface to get extra buttons provided by the main main of a custom app.
 * Concrete implementation is provided by the app
 * and the name of that class is specified in R.string.ou_main_menu_provider .
 */
public abstract class MenuProvider {
    private static final String TAG = MenuProvider.class.getSimpleName();

    static public MenuProvider create(String clazzName) {
        if (TextUtils.isEmpty(clazzName)) {
            return null;
        }
        try {
            Class clazz = Class.forName(clazzName);
            return (MenuProvider) clazz.newInstance();
        } catch (Exception e) {
            Analytics.logException(e);
            return null;
        }
    }

    abstract public void onCreate(Activity activity);
    abstract public boolean onItemSelected(Activity activity, int id, String via);
}
