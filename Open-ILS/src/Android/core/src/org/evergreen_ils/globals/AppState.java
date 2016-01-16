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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import org.evergreen_ils.R;

import java.io.File;
import java.util.Date;

/** App State that is persistent across invocations; stored as preferences.
 *
 * Created by kenstir on 11/8/2015.
 */
public class AppState {
    private static final String TAG = AppState.class.getSimpleName();
    public static final String LAUNCH_COUNT = "launch_count";
    public static final String LIBRARY_URL = "library_url";
    public static final String LIBRARY_NAME = "library_name";

    // increment PREFS_VERSION every time you make a change to the persistent pref storage
    private static final int PREFS_VERSION = 2;
    private static final String VERSION = "version";
    private static Context context;
    private static boolean initialized;

    public static void init(Context callingContext) {
        if (initialized)
            return;

        context = callingContext.getApplicationContext();
        initialized = true;

        // set default values unless already set
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int version = prefs.getInt(VERSION, 0);
        if (version < PREFS_VERSION) {
            SharedPreferences.Editor editor = prefs.edit();
            version = PREFS_VERSION;
            editor.putInt(VERSION, PREFS_VERSION);
            editor.putString(LIBRARY_URL, context.getString(R.string.ou_library_url));
            editor.putString(LIBRARY_NAME, context.getString(R.string.ou_library_name));
            editor.commit();
        }
        Log.d(TAG, "version=" + version);
        Log.d(TAG, "library_url=" + getString(LIBRARY_URL));
        Log.d(TAG, "library_name=" + getString(LIBRARY_NAME));
    }

    public static long getFirstInstallTime() {
        try {
            String packageName = context.getPackageName();
            long time = context.getPackageManager().getPackageInfo(packageName, 0).firstInstallTime;
            Log.d(TAG, "firstInstallTime=" + time);
            Log.d(TAG, "firstInstallTime " + new Date(time).toLocaleString());
            return time;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "caught", e);
            return 0;
        }
    }

    public static String getString(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, null);
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean dflt) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, dflt);
    }

    public static int getInt(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key, 0);
    }

    public static void setString(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void setBoolean(String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void setInt(String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }
}