package org.evergreen_ils.utils.ui;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by kenstir on 12/5/2017.
 */

public class CrashUtils {
    public static void onCreate(Context context) {
        Fabric.with(context, new Crashlytics());
    }
}
