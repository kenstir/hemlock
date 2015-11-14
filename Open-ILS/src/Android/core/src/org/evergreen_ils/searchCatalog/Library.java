package org.evergreen_ils.searchCatalog;

import android.text.TextUtils;

/** value class
 * Created by kenstir on 11/5/2015.
 */
public class Library {
    public String url;            // e.g. "https://catalog.cwmars.org"
    public String short_name;     // e.g. "C/W MARS"
    public String directory_name; // e.g. "Massachusetts, US (C/W MARS)"
    public Library(String url, String short_name, String directory_name) {
        this.url = url;
        this.short_name = short_name;
        this.directory_name = directory_name;
    }
}
