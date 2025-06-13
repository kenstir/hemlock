//
//  Copyright (C) 2019 Kenneth H. Cox
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.evergreen_ils.android;

import android.content.res.Resources;
import android.text.TextUtils;

import net.kenstir.hemlock.R;

public class AppFactory {
    private static final String TAG = AppFactory.class.getSimpleName();

    static public AppBehavior makeBehavior(Resources resources) {
        String clazzName = resources.getString(R.string.ou_behavior_provider);
        if (!TextUtils.isEmpty(clazzName)) {
            try {
                Class clazz = Class.forName(clazzName);
                return (AppBehavior) clazz.newInstance();
            } catch (Exception e) {
                Analytics.logException(e);
            }
        }
        return new AppBehavior();
    }
}
