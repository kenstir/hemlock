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

package net.kenstir.ui.util;

import android.text.TextUtils;

import net.kenstir.hemlock.R;
import net.kenstir.ui.App;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.kenstir.ui.AppState;

public class ActionBarUtils {

    public static Toolbar initActionBarForActivity(AppCompatActivity activity, String title, boolean isMainActivity) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null)
            return toolbar;
        String username = App.getAccount().getDisplayName();
        if (activity.getResources().getBoolean(R.bool.admin_screenshot_mode))
            username = "janejetson";
        actionBar.setSubtitle(String.format(activity.getString(R.string.ou_activity_subtitle),
                AppState.getString(AppState.LIBRARY_NAME), username));
        if (!TextUtils.isEmpty(title))
            actionBar.setTitle(title);
        if (true || !isMainActivity) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(0);
        }

        return toolbar;
    }

    public static void initActionBarForActivity(AppCompatActivity activity, String title) {
        initActionBarForActivity(activity, title, false);
    }

    public static void initActionBarForActivity(AppCompatActivity activity) {
        initActionBarForActivity(activity, null, false);
    }

}
