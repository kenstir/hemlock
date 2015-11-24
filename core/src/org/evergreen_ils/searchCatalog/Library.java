package org.evergreen_ils.searchCatalog;

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
