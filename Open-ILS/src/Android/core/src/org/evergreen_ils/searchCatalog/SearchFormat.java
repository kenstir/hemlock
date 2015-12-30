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

package org.evergreen_ils.searchCatalog;

import android.content.Context;
import android.text.TextUtils;
import org.evergreen_ils.R;
import org.evergreen_ils.globals.Log;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Support translation between search_format strings ("book"), display labels as seen in the search format spinner
 * ("All Books"), and display labels as shown on an item detail page ("Book").
 *
 * Created by kenstir on 10/17/2015.
 */
public class SearchFormat {

    private static class SearchFormatItem {
        public String spinnerLabel = null;
        public String displayLabel = null;
        public String searchFormat = null;
        public boolean hidden;
    }

    private final static String TAG = SearchFormat.class.getSimpleName();

    static boolean initialized = false;
    static List<SearchFormatItem> searchFormats = new ArrayList<SearchFormatItem>();

    public static void init(Context context) {
        if (initialized) return;
        String formats_json = loadJSONFromResource(context, R.raw.search_formats);
        SearchFormat.initFromJSON(formats_json);
    }

    private static String loadJSONFromResource(Context context, int r) {
        String json = "";
        InputStream is = context.getResources().openRawResource(r);
        int size = 0;
        try {
            size = is.available();
            byte[] buf = new byte[size];
            is.read(buf);
            is.close();
            json = new String(buf, "UTF-8");
        } catch (IOException e) {
            Log.d(TAG, "caught", e);
        }
        return json;
    }

    public static void initFromJSON(String formats_json) {
        searchFormats.clear();

        List<Map<String,?>> formatsList;
        try {
            formatsList = (List<Map<String, ?>>) new JSONReader(formats_json).readArray();
        } catch (JSONException e) {
            Log.d(TAG, "failed parsing json: "+formats_json, e);
            return;
        }
        for (int i=0; i<formatsList.size(); ++i) {
            Map<String, ?> m = formatsList.get(i);
            Log.d(TAG, "item:"+m);
            SearchFormatItem item = new SearchFormatItem();
            item.spinnerLabel = (String)m.get("l");
            item.displayLabel = (String)m.get("L");
            item.searchFormat = (String)m.get("f");
            item.hidden = m.containsKey("h");
            searchFormats.add(item);
        }
    }

    /// list of labels e.g. "All Formats", "All Books", ...
    public static List<String> getSpinnerLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for (SearchFormatItem i : searchFormats) {
            if (!i.hidden) {
                labels.add(i.spinnerLabel);
            }
        }
        return labels;
    }

    /// get search format "music" from label "All Music"
    public static String getSearchFormatFromSpinnerLabel(String label) {
        for (SearchFormatItem i : searchFormats) {
            if (i.spinnerLabel.equalsIgnoreCase(label)) {
                return i.searchFormat;
            }
        }
        Log.w(TAG, "label not found: "+label);
        return "";
    }

    /// get label "CD Music recording" from search format "cdmusic"
    public static String getItemLabelFromSearchFormat(String search_format) {
        if (TextUtils.isEmpty(search_format))
            return "";
        for (SearchFormatItem i : searchFormats) {
            if (i.searchFormat.equalsIgnoreCase(search_format)) {
                return (i.displayLabel != null) ? i.displayLabel : i.spinnerLabel;
            }
        }
        Log.w(TAG, "search format not found: "+search_format);
        return "";
    }
}
