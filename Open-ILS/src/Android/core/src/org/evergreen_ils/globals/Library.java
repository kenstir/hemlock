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

package org.evergreen_ils.globals;

import android.location.Location;
import android.text.TextUtils;

/** value class
 * Created by kenstir on 11/5/2015.
 */
public class Library {
    public String url;            // e.g. "https://catalog.cwmars.org"
    public String name;           // e.g. "C/W MARS"
    public String directory_name; // e.g. "Massachusetts, US (C/W MARS)"
    public Location location;
    public Library(String url, String name, String directory_name, Location location) {
        this.url = url;
        this.name = name;
        this.directory_name = directory_name;
        this.location = location;
    }
    public Library(String url, String name) {
        this(url, name, null, null);
    }
}
