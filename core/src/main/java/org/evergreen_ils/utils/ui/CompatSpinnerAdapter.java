/*
 * Copyright (C) 2015 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.utils.ui;

import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;
import org.evergreen_ils.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kenstir on 1/5/14.
 */
public class CompatSpinnerAdapter {
    /** Create an ArrayAdapter for use in a spinner.
     * This solves the problem running under theme Theme.AppCompat: under Gingerbread, the spinner dialog
     * has a white background, even though the theme is dark, and the text is white-on-white.
     * The fix is to use two different spinner layouts, one for GB with textColor="@color/dark"
     * and one for newer versions with textColor="?android:textColorPrimary".
     */
    public static ArrayAdapter<String> CreateCompatSpinnerAdapter(Context context, List<String> list) {
        int spinner_layout = (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                ? R.layout.spinner_gb_layout :
                R.layout.spinner_layout;
        ArrayAdapter<String> adapter  = new ArrayAdapter<String>(context, spinner_layout, list);
        return adapter;
    }
}
