package org.evergreen_ils.globals;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.bookbags.BookBag;

/**
 * Created by kenstir on 11/8/2015.
 */
public class AppPrefs {
    public static final String LIBRARY_URL = "library_url";
    public static final String LIBRARY_NAME = "library_name";
    private static final String VERSION = "version";
    // increment PREFS_VERSION every time you make a change to the persistent pref storage
    private static final int PREFS_VERSION = 2;
    private static final String TAG = AppPrefs.class.getSimpleName();
    private static Context context;
    private static boolean initialized;

    public static void init(Context app_context) {
        if (initialized)
            return;

        context = app_context;
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

    public static String getString(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, null);
    }

    public static boolean getBoolean(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
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
}
