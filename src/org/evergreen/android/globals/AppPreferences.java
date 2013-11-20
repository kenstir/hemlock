package org.evergreen.android.globals;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppPreferences {
    public static final String KEY_PREFS_LAST_ACCOUNT_NAME = "account_name";
    private static final String APP_SHARED_PREFS = AppPreferences.class.getSimpleName();
    private SharedPreferences _sharedPrefs;
    private Editor _prefsEditor;

    public AppPreferences(Context context) {
        this._sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this._prefsEditor = _sharedPrefs.edit();
    }

    public String getLastAccountName() {
        return _sharedPrefs.getString(KEY_PREFS_LAST_ACCOUNT_NAME, "");
    }

    public void putLastAccountName(String text) {
        _prefsEditor.putString(KEY_PREFS_LAST_ACCOUNT_NAME, text);
        _prefsEditor.commit();
    }
    
    public void clearLastAccountName() {
        putLastAccountName(null);
    }
}
