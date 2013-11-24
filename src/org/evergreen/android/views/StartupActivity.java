package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.LoginController;
import org.evergreen.android.globals.AppPreferences;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.JunkActivity;
import org.evergreen_ils.auth.Const;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class StartupActivity extends Activity {

    private String TAG = StartupActivity.class.getSimpleName();
    private TextView mLoginStatusMessageView;
    private String mAlertMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_progress_message);
        
        LoginController.getInstance(this).loginForActivity(JunkActivity.class);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.startup, menu);
        return true;
    }
    */
}
