package org.evergreen_ils.auth;

import org.evergreen_ils.auth.Authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent arg0) {
        return new Authenticator(this).getIBinder();
    }
}
