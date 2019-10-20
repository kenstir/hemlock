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

import org.evergreen_ils.Api;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.utils.TextUtils;
import org.opensrf.ShouldNotHappenException;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodedValueMap {
    public static final String SEARCH_FORMAT = "search_format";
    public static final String ICON_FORMAT = "icon_format";
    public static final String ALL_SEARCH_FORMATS = "All Formats";

    static class CodedValue {
        public String code;
        public String value;
        public boolean opac_visible;
        public CodedValue(String code, String value, boolean opac_visible) {
            this.code = code;
            this.value = value;
            this.opac_visible = opac_visible;
        }
    }

    static ArrayList<CodedValue> search_formats = new ArrayList<>();
    static ArrayList<CodedValue> icon_formats = new ArrayList<>();

    public static void loadCodedValueMaps(List<OSRFObject> objects) {
        search_formats = new ArrayList<>();
        icon_formats = new ArrayList<>();

        for (OSRFObject obj: objects) {
            String ctype = obj.getString("ctype", "");
            String code = obj.getString("code","");
            Boolean opac_visible = Api.parseBoolean(obj.get("opac_visible"));
            String search_label = obj.getString("search_label","");
            String value = obj.getString("value", "");
            CodedValue cv = new CodedValue(code, !TextUtils.isEmpty(search_label) ? search_label : value, opac_visible);
            if (ctype.equals(SEARCH_FORMAT)) {
                search_formats.add(cv);
            } else if (ctype.equals(ICON_FORMAT)) {
                icon_formats.add(cv);
            }
        }
    }

    static String getValueFromCode(String ctype, String code) {
        ArrayList<CodedValue> values;
        if (ctype.equals(SEARCH_FORMAT)) {
            values = search_formats;
        } else if (ctype.equals(ICON_FORMAT)) {
            values = icon_formats;
        } else {
            return null;
        }
        for (CodedValue cv: values) {
            if (TextUtils.equals(code, cv.code)) {
                return cv.value;
            }
        }
        Analytics.logException(new ShouldNotHappenException("Unknown ccvm code: "+code));
        return null;
    }

    static String getCodeFromValue(String ctype, String value) {
        ArrayList<CodedValue> values;
        if (ctype.equals(SEARCH_FORMAT)) {
            values = search_formats;
        } else if (ctype.equals(ICON_FORMAT)) {
            values = icon_formats;
        } else {
            return null;
        }
        for (CodedValue cv: values) {
            if (TextUtils.equals(value, cv.value)) {
                return cv.code;
            }
        }
        Analytics.logException(new ShouldNotHappenException("Unknown ccvm value: "+value));
        return null;
    }

    public static String iconFormatLabel(String code) {
        return getValueFromCode(ICON_FORMAT, code);
    }

    public static String searchFormatLabel(String code) {
        return getValueFromCode(SEARCH_FORMAT, code);
    }

    public static String searchFormatCode(String label) {
        if (TextUtils.isEmpty(label) || TextUtils.equals(label, ALL_SEARCH_FORMATS))
            return "";
        return getCodeFromValue(SEARCH_FORMAT, label);
    }

    /// list of labels e.g. "All Formats", "All Books", ...
    public static List<String> getSearchFormatSpinnerLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        for (CodedValue cv : search_formats) {
            if (cv.opac_visible) {
                labels.add(cv.value);
            }
        }
        Collections.sort(labels);
        labels.add(0, ALL_SEARCH_FORMATS);
        return labels;
    }
}
