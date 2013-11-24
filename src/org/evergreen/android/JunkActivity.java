package org.evergreen.android;

import org.evergreen.android.accountAccess.LoginController;

import android.app.Activity;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JunkActivity extends Activity {
    
    private Button b;
    //private TextView tv;
    private EditText t,t2;
    private String auth_token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_junk);
        b = (Button) findViewById(R.id.junkButton);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (auth_token == null) {
                    LoginController.getInstance(JunkActivity.this).login();
                }
            }
        });
        t = (EditText) findViewById(R.id.junkTokenText);
        t2 = (EditText) findViewById(R.id.junkUsernameText);
        String account_name = LoginController.getInstance(this).getAccountName();
        auth_token = LoginController.getInstance(this).getAuthToken();
        t.setText((auth_token==null)?"null":auth_token);
        t2.setText((account_name==null)?"null":account_name);
        
        if (auth_token == null) {
            LoginController.getInstance(this).login();
        }
    }
}
