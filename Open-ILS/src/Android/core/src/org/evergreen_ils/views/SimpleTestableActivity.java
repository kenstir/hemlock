package org.evergreen_ils.views;

import android.app.Activity;
import android.os.Bundle;

/** Simple activity that serves to run tests based on ActivityInstrumentationTestCase2.
 * The app activities like MainActivity can't be run from there because they forward
 * to SplashActivity and prompt for auth etc.
 *
 * Created by kenstir on 12/5/2015.
 */
public class SimpleTestableActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}