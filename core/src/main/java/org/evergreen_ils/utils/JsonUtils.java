package org.evergreen_ils.utils;

import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JsonUtils {
    /** Parse a metarecord hold attribute "holdable_formats" into a list of ccvm codes */
    public static ArrayList<String> parseHoldableFormats(Map<String, Object> dict) {
        ArrayList<String> formats = new ArrayList<>();
        if (dict == null)
            return formats;
        for (Map.Entry<String, Object> entry : dict.entrySet()) {
            ArrayList<Map<String, String>> l = (ArrayList<Map<String, String>>) entry.getValue();
            for (Map<String, String> e : l) {
                String attr = e.get("_attr");
                String value = e.get("_val");
                if (TextUtils.equals(attr, "mr_hold_format") && value != null) {
                    formats.add(value);
                }
            }
        }
        return formats;
    }

    public static Map<String, Object> parseObject(String json) {
        try {
            return (Map<String, Object>) new JSONReader(json).readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
