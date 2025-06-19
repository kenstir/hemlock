package org.opensrf;

import net.kenstir.hemlock.android.Analytics;

/**
 * Used to log unexpected events to Crashlytics.
 */
public class ShouldNotHappenException extends Exception {
    public ShouldNotHappenException(String message) {
        super(message);
    }
    public ShouldNotHappenException(Integer githubIssueId, String message) {
        super("Issue #"+githubIssueId+": "+message);
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

