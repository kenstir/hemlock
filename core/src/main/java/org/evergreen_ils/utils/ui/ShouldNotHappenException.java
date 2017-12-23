package org.evergreen_ils.utils.ui;

import org.evergreen_ils.system.Analytics;
import org.opensrf.Method;

/**
 * Used to log unexpected events to Crashlytics.
 */
public class ShouldNotHappenException extends Exception {
    public ShouldNotHappenException(String message) {
        super(message);
    }
    public ShouldNotHappenException(String service, Method method) {
        Analytics.setString("svc", service);
        Analytics.setString("m", method.getName());
        Analytics.setString("params", "" + method.getParams());
    }
    public ShouldNotHappenException(Throwable cause) {
        super(cause);
    }
}

