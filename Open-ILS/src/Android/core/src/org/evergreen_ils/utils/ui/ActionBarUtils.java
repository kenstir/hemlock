package org.evergreen_ils.utils.ui;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.globals.AppPrefs;

/**
 * Created by kenstir on 11/21/2015.
 */
public class ActionBarUtils {
    public static void initActionBarForActivity(ActionBarActivity activity, boolean isMainActivity) {
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setSubtitle(AppPrefs.getString(AppPrefs.LIBRARY_NAME) + " - " + AccountAccess.userName);
        if (!isMainActivity) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static void initActionBarForActivity(ActionBarActivity activity) {
        initActionBarForActivity(activity, false);
    }
}
