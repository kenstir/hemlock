package org.evergreen.android;

import org.evergreen.android.accountAccess.CurrentLogin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class JunkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        String account_name = CurrentLogin.getUsername();
        String auth_token = CurrentLogin.getAuthToken();
        tv.setText("account.name="+((account_name==null)?"null":account_name)
                + "\nauth_token="+((auth_token==null)?"null":auth_token));
        setContentView(tv);
    }
}
