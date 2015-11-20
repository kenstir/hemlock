package org.evergreen_ils.auth;

import android.util.Log;
import org.evergreen_ils.auth.AccountAuthenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(Const.AUTH_TAG, "onBind intent:"+arg0);
        return new AccountAuthenticator(this).getIBinder();
    }
}
