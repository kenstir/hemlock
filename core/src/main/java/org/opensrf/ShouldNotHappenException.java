package org.opensrf;

import com.crashlytics.android.Crashlytics;

/**
 * Used to log unexpected events to Crashlytics.
 */
public class ShouldNotHappenException extends Exception {
    public ShouldNotHappenException(String message) {
        super(message);
    }
    public ShouldNotHappenException(String service, Method method) {
        Crashlytics.setString("svc", service);
        Crashlytics.setString("m", method.getName());
        Crashlytics.setString("params", "" + method.getParams());
    }
    public ShouldNotHappenException(Throwable cause) {
        super(cause);
    }
}

