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

package net.kenstir.hemlock.data.evergreen;

import android.annotation.SuppressLint;

import net.kenstir.hemlock.android.Analytics;
import org.opensrf.ShouldNotHappenException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OSRFUtils {
    public static final String API_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String API_DAY_ONLY_PATTERN = "yyyy-MM-dd";
    public static final String API_HOURS_PATTERN = "HH:mm:ss";
    public static final String OUTPUT_DATE_PATTERN = "MM/dd/yyyy";
    public static final String OUTPUT_DATE_TIME_PATTERN = "MM/dd/yyyy h:mm a";

    /** format ISO date+time string to pass to API methods */
    public static String formatDate(Date date) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(API_DATE_PATTERN);
        return df.format(date);
    }

    /** format ISO date string yyyy-MM-dd to pass to API methods */
    public static String formatDateAsDayOnly(Date date) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(API_DAY_ONLY_PATTERN);
        return df.format(date);
    }

    /** parse ISO date+time string returned from API methods */
    public static Date parseDate(@Nullable String dateString) {

        if (dateString == null)
            return null;

        Date date;
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(API_DATE_PATTERN);

        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            Analytics.logException(e);
            date = new Date();
        }

        return date;
    }

    /** parse time string HH:MM:SS returned from API */
    public static @Nullable
    Date parseHours(String hoursString) {
        if (hoursString == null)
            return null;

        Date date;
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(API_HOURS_PATTERN);

        try {
            date = df.parse(hoursString);
        } catch (ParseException e) {
            date = new Date();
        }

        return date;
    }

    public static String formatHoursForOutput(@NonNull Date date) {
        // Use the default locale instead of fixed AM/PM, even though
        // this will make the tests break when run in a non-US locale.
        DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        return timeFormatter.format(date);
    }

    public static String formatDateForOutput(@NonNull Date date) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(OUTPUT_DATE_PATTERN);
        return df.format(date);
    }

    public static String formatDateTimeForOutput(@NonNull Date date) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat df = new SimpleDateFormat(OUTPUT_DATE_TIME_PATTERN);
        return df.format(date);
    }

    // parse bool string returned from API methods
    public static Boolean parseBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            String s = (String) obj;
            return s.equals("t");
        } else {
            return false;
        }
    }

    /**
     * Return o as an Integer
     *
     * Sometimes search returns a count as a json number ("count":0), sometimes a string ("count":"1103").
     * Seems to be the same for result "ids" list (See Issue #1).  Handle either form and return as an int.
     */
    @Nullable
    public static Integer parseInt(Object o, Integer defaultValue) {
        if (o == null) {
            return defaultValue;
        } else if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof String) {
            // I have seen settings with value "", e.g. opac.default_sms_carrier
            try {
                return Integer.parseInt((String) o);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            Analytics.logException(new ShouldNotHappenException("unexpected type: "+o));
            return defaultValue;
        }
    }

    @Nullable
    public static Integer parseInt(Object o) {
        return parseInt(o, null);
    }

    public static @NonNull List<Integer> parseIdsListAsInt(Object o) {
        ArrayList<Integer> ret = new ArrayList<>();
        if (o instanceof List) {
            for (Object elem: (List<?>) o) {
                Integer i = parseInt(elem);
                if (i != null) {
                    ret.add(i);
                }
            }
        }
        return ret;
    }
}
