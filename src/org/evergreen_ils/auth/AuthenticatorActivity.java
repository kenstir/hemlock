package org.evergreen_ils.auth;

import org.evergreen.android.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private final String TAG = "eg.auth";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    //public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String PARAM_USER_PASS = "USER_PASS";
    private final int REQ_SIGNUP = 1;
    private static final String STATE_ALERT_MESSAGE = "state_dialog";

    private AccountManager accountManager;
    private String authTokenType;
    private AsyncTask task = null;
    private AlertDialog alertDialog = null;
    private String alertMessage = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        accountManager = AccountManager.get(getBaseContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        Log.d(TAG, "onCreate> accountName="+accountName);
        authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (authTokenType == null)
            authTokenType = Const.AUTHTOKEN_TYPE;
        Log.d(TAG, "onCreate> authTokenType="+authTokenType);

        if (accountName != null) {
            ((TextView) findViewById(R.id.accountName)).setText(accountName);
        }

        findViewById(R.id.submit).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submit();
                    }
                });
        /*
         * findViewById(R.id.signUp).setOnClickListener(new
         * View.OnClickListener() {
         * 
         * @Override public void onClick(View v) { // Since there can only be
         * one AuthenticatorActivity, we call the sign up activity, get his
         * results, // and return them in setAccountAuthenticatorResult(). See
         * finishLogin(). Intent signup = new Intent(getBaseContext(),
         * SignUpActivity.class); signup.putExtras(getIntent().getExtras());
         * startActivityForResult(signup, REQ_SIGNUP); } });
         */
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate> savedInstanceState="+savedInstanceState);
            if (savedInstanceState.getString(STATE_ALERT_MESSAGE) != null) {
                showAlert(savedInstanceState.getString(STATE_ALERT_MESSAGE));
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (alertMessage != null) {
            outState.putString(STATE_ALERT_MESSAGE, alertMessage);
        }
        if (task != null) {
            Log.d(TAG, "onSaveInstanceState> we have task, should we cancel it?");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult> requestCode="+requestCode+" resultCode="+resultCode);
        // The sign up activity returned that the user has successfully created
        // an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            onAuthSuccess(data);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void submit() {
        Log.d(TAG, "submit>");

        final String username = ((TextView) findViewById(R.id.accountName)).getText().toString();
        final String password = ((TextView) findViewById(R.id.accountPassword)).getText().toString();
        //final String account_type = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        task = new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {

                Log.d(TAG, "task> Start authenticating");

                String authtoken = null;
                String errorMessage = "Login failed";
                Bundle data = new Bundle();
                try {
                    authtoken = EvergreenAuthenticator.signIn(AuthenticatorActivity.this, username, password);
                    Log.d(TAG, "task> signIn returned "+authtoken);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, Const.ACCOUNT_TYPE);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(PARAM_USER_PASS, password);
                } catch (AuthenticationException e) {
                    if (e != null) errorMessage = e.getMessage();
                    Log.d(TAG, "task> signIn caught auth exception "+errorMessage);
                } catch (Exception e2) {
                    if (e2 != null) errorMessage = e2.getMessage();
                    Log.d(TAG, "task> signIn caught other exception "+errorMessage);
                }
                if (authtoken == null)
                    data.putString(KEY_ERROR_MESSAGE, errorMessage);

                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                task = null;
                Log.d(TAG, "task.onPostExecute> intent="+intent);
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Log.d(TAG, "task.onPostExecute> error msg: "+intent.getStringExtra(KEY_ERROR_MESSAGE));
                    onAuthFailure(intent.getStringExtra(KEY_ERROR_MESSAGE));
                } else {
                    Log.d(TAG, "task.onPostExecute> no error msg");
                    onAuthSuccess(intent);
                }
            }
        }.execute();
    }

    protected void onAuthFailure(String errorMessage) {
        showAlert(errorMessage);
    }
    
    protected void showAlert(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        alertMessage = errorMessage;
        alertDialog = builder.setTitle("Login failed")
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog = null;
                        alertMessage = null;
                    }
                })
                .create();
        alertDialog.show();
    }

    private void onAuthSuccess(Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, accountType);
        Log.d(TAG, "onAuthSuccess> accountName="+accountName);

        //if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
        Log.d(TAG, "onAuthSuccess> addAccountExplicitly "+accountName);
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = authTokenType;

        // Create the account on the device
        if (accountManager.addAccountExplicitly(account, accountPassword, null)) {
            Log.d(TAG, "onAuthSuccess> true, setAuthToken "+authtoken);
            // Not setting the auth token will cause another call to the server
            // to authenticate the user
            accountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            // Probably the account already existed, in which case update the password
            Log.d(TAG, "onAuthSuccess> false, setPassword");
            accountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
