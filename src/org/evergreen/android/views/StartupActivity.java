package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.CurrentLogin;
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
    private StartupTask mStartupTask = null;
    private String mAlertMessage = null;
    private AppPreferences mAppPrefs;
    private AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        
        mAppPrefs = new AppPreferences(this);
        mAccountManager = AccountManager.get(this);
        
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        
        signIn();
    }
    
    public void signIn() {
        final String auth_token = CurrentLogin.getAuthToken();
        if (TextUtils.isEmpty(auth_token)) {
            getTokenForLastActiveAccount();
        } else {
            Log.d(TAG, "signIn> already have auth_token");
            initializeAccountInfo();
        }
    }

    private void getTokenForLastActiveAccount() {
        final String username = mAppPrefs.getLastAccountName();
        Log.d(TAG, "getToken> username="+username);
        
        // first try to get an auth token for the last account used
        if (!TextUtils.isEmpty(username)) {
            if (reuseExistingAccountAuthToken(username)) {
                Log.d(TAG, "getToken> reuseExisting returned true");
            }
        } else {
            getTokenForAccountCreateIfNeeded();
        }
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

    /**
     * Add new account to the account manager
     */
    private void addNewAccount() {
        //final AccountManagerFuture<Bundle> future = 
        mAccountManager.addAccount(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    showMessage("Account "+account_name+" was created");
                    mAppPrefs.putLastAccountName(account_name);
                    Log.d(TAG, "addNewAccount bnd=" + bnd);
                } catch (Exception e) {
                    mAppPrefs.clearLastAccountName();
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }, null);
    }

    /**
     * Show all the accounts registered on the account manager. Request an auth token upon user select.
     * @param authTokenType
     */
    private void showAccountPicker() {
        final Account availableAccounts[] = mAccountManager.getAccountsByType(Const.ACCOUNT_TYPE);

        if (availableAccounts.length == 0) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show();
            addNewAccount();
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= 1/*Build.VERSION_CODES.HONEYCOMB*/) {
                builder = new AlertDialog.Builder(this);
            } else {
                ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.EvergreenTheme);
                builder = new AlertDialog.Builder(wrapper);
            }
            AlertDialog aDialog = builder.setTitle("Pick Account").setItems(name, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getExistingAccountAuthToken(availableAccounts[which]);
                }
            }).create();
            aDialog.show();
        }
    }

    private void getExistingAccountAuthToken(final Account account) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, this, 
                new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                Log.d(TAG, "getExistingAccountAuthToken> callback run> got future "+future);
            }
        }, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();
                    Log.d(TAG, "getExistingAccountAuthToken> thread run> got future "+future);
                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    onSuccessfulLogin(account_name, authtoken);
                } catch (Exception e) {
                    Log.d(TAG, "getExistingAccountAuthToken caught "+e.getMessage());
                    onFailedLogin(e.getMessage());
                }
            }
        }).start();
    }

    protected void onSuccessfulLogin(String account_name, String auth_token) {
        Log.d(TAG,"onSuccessfulLogin> account_name "+account_name+" token "+auth_token);
        showMessage((auth_token != null) ? "SUCCESS with "+account_name+"\ntoken: " + auth_token : "FAIL");
        if (auth_token != null) {
            mAppPrefs.putLastAccountName(account_name);
            CurrentLogin.setAccountInfo(account_name, auth_token);
            startNextActivity();
        }
    }
    
    protected void onFailedLogin(String msg) {
        mAppPrefs.clearLastAccountName();
        CurrentLogin.clear();
        showMessage(msg);
    }

    private boolean reuseExistingAccountAuthToken(final String account_name) {
        final Account availableAccounts[] = mAccountManager.getAccountsByType(Const.ACCOUNT_TYPE);
        for (int i = 0; i < availableAccounts.length; i++) {
            Log.d(TAG, "reuseExistingAccountAuthToken> looking for "+account_name+", found "+availableAccounts[i].name);
            if (account_name.equals(availableAccounts[i].name)) {
                Log.d(TAG, "reuseExistingAccountAuthToken> found it");
                getExistingAccountAuthToken(availableAccounts[i]);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Invalidates the auth token for the account
     * @param account
     * @param authTokenType
     */
    private void invalidateAuthToken(final Account account) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, this, null,null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();

                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    mAccountManager.invalidateAuthToken(account.type, authtoken);
                    showMessage(account.name + " invalidated");
                    mAppPrefs.putLastAccountName(null);
                } catch (Exception e) {
                    mAppPrefs.putLastAccountName(null);
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Get an auth token for the account.
     * If not exist - add it and then return its auth token.
     * If one exist - return its auth token.
     * If more than one exists - show a picker and return the select account's auth token.
     * @param accountType
     * @param authTokenType
     */
    private void getTokenForAccountCreateIfNeeded() {
        Log.d(TAG, "getTokenForAccountCreateIfNeeded> ");
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, this, null, null,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bnd = null;
                        try {
                            bnd = future.getResult();
                            Log.d(TAG, "getTokenForAccountCreateIfNeeded> bnd="+bnd);
                            final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                            final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                            onSuccessfulLogin(account_name, authtoken);
                        } catch (Exception e) {
                            Log.d(TAG, "getTokenForAccountCreateIfNeeded> caught "+e.getMessage());
                            onFailedLogin(e.getMessage());
                        }
                    }
                }
        , null);
    }

    private void showMessage(final String msg) {
        if (TextUtils.isEmpty(msg))
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "showMessage> "+msg);
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNextActivity() {
        //Intent intent = new Intent(this, SearchCatalogListView.class);
        Intent intent = new Intent(this, JunkActivity.class);
        startActivity(intent);
        finish();
    }

    public void downloadResources() {
        if (mStartupTask != null) {
            return;
        }
        
        // blah blah blah
        // mDownloadTask = new LoginTask();
    }

    private class StartupTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // TODO
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            return true;
        }
        
        @Override
        protected void onProgressUpdate(Void... params) {
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mStartupTask = null;
            if (success) {
                startNextActivity();
            }
        }

        @Override
        protected void onCancelled() {
            mStartupTask = null;
        }
    }
}
