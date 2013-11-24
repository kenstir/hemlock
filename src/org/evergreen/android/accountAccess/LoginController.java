package org.evergreen.android.accountAccess;

import org.evergreen.android.JunkActivity;
import org.evergreen.android.R;
import org.evergreen.android.globals.AppPreferences;
import org.evergreen.android.views.StartupActivity;
import org.evergreen_ils.auth.Const;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

/**
 * Handle common Activity startup: making sure we have an account, and an auth_token.
 * 
 * @author kenstir
 *
 */
public class LoginController {
    
    protected static LoginController mInstance = null;

    private String TAG = LoginController.class.getSimpleName();
    protected String mAccountName = null;
    protected String mAuthToken = null; 
    protected StartupTask mStartupTask = null;
    private Activity mActivity = null;
    private Class<?> mNextActivityClass = null;
    private AppPreferences mAppPrefs = null;
    private AccountManager mAccountManager;

    protected LoginController() {
    }
    
    protected void setActivity(Activity a) {
        mActivity = a;
        if (mAppPrefs == null) mAppPrefs = new AppPreferences(a);
        if (mAccountManager == null) mAccountManager = AccountManager.get(a);
    }
    
    public static LoginController getInstance(Activity a) {
        if (mInstance == null)
            mInstance = new LoginController();
        mInstance.setActivity(a);
        return mInstance;
    }
    
    public static LoginController getInstance() {
        return mInstance;
    }

    public static String getAccountName() {
        return getInstance().mAccountName;
    }

    public static String getAuthToken() {
        return getInstance().mAuthToken;
    }

    /** login and stay on the current activity, for use by non-startup activity */
    public void login() {
        loginForActivity(null);
    }

    /** login and forward to another activity, for use by startup/splash activity */
    public void loginForActivity(Class nextActivity) {
        mNextActivityClass = nextActivity;

        final String auth_token = getAuthToken();
        if (TextUtils.isEmpty(auth_token)) {
            getTokenForLastActiveAccount();
        } else {
            Log.d(TAG, "signIn> already have auth_token");
            startNextActivity();
        }
    }
    
    private void getTokenForLastActiveAccount() {
        final String username = mAppPrefs.getLastAccountName();
        Log.d(TAG, "getTokenForLastActiveAccount> username="+username);
        
        // first try to get an auth token for the last account used
        if (!TextUtils.isEmpty(username) && reuseExistingAccountAuthToken(username)) {
            Log.d(TAG, "getTokenForLastActiveAccount> reuseExisting returned true");
        } else {
            getTokenForAccountCreateIfNeeded();
        }
    }

    /**
     * Add new account to the account manager
     */
    private void addNewAccount() {
        //final AccountManagerFuture<Bundle> future = 
        mAccountManager.addAccount(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, null, mActivity, new AccountManagerCallback<Bundle>() {
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
            Toast.makeText(mActivity, "No accounts", Toast.LENGTH_SHORT).show();
            addNewAccount();
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= 1/*Build.VERSION_CODES.HONEYCOMB*/) {
                builder = new AlertDialog.Builder(mActivity);
            } else {
                ContextThemeWrapper wrapper = new ContextThemeWrapper(mActivity, R.style.EvergreenTheme);
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
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, mActivity, 
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
            mAccountName = account_name;
            mAuthToken = auth_token;
            startNextActivity();
        }
    }
    
    protected void onFailedLogin(String msg) {
        mAppPrefs.clearLastAccountName();
        mAccountName = null;
        mAuthToken = null;
        showMessage(msg);
    }
    
    protected void onInvalidateAuthToken() {
        mAuthToken = null;
    }

    private boolean reuseExistingAccountAuthToken(final String account_name) {
        Log.d(TAG, "reuseExistingAccountAuthToken> looking for "+account_name);
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
     * Invalidates the current auth token for the account
     */
    public void invalidateAuthToken() {
        if (mAuthToken != null) {
            mAccountManager.invalidateAuthToken(Const.ACCOUNT_TYPE, mAuthToken);
            onInvalidateAuthToken();
        }
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
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, mActivity, null, null,
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

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "showMessage> "+msg);
                Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNextActivity() {
        //Intent intent = new Intent(this, SearchCatalogListView.class);
        Intent intent = new Intent(mActivity, mNextActivityClass);
        mActivity.startActivity(intent);
        mActivity.finish();
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
