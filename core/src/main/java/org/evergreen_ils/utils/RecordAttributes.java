/*
 * Copyright (C) 2019 Kenneth H. Cox
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

package org.evergreen_ils.utils;

import android.text.TextUtils;

import org.opensrf.util.OSRFObject;

import java.util.HashMap;

public class RecordAttributes {
    public static HashMap<String, String> parseAttributes(OSRFObject mra_obj) {
        if (mra_obj == null)
            return new HashMap<>();
        String attrs = mra_obj.getString("attrs");
        if (TextUtils.isEmpty(attrs))
            return new HashMap<>();
        return parseAttributes(attrs);
    }

    public static HashMap<String, String> parseAttributes(String attrs_dump) {
        HashMap<String, String> map = new HashMap<>();
        String[] attr_arr = TextUtils.split(attrs_dump, ", ");
        for (int i=0; i<attr_arr.length; ++i) {
            String[] kv = TextUtils.split(attr_arr[i], "=>");
            String k = kv[0].replace("\"", "");
            String v = kv[1].replace("\"", "");
            map.put(k, v);
        }
        return map;
    }
}
