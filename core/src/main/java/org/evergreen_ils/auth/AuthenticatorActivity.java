package org.evergreen_ils.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.evergreen_ils.android.App;

import org.evergreen_ils.R;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.data.Library;
import org.evergreen_ils.utils.ui.AppState;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private final static String TAG = AuthenticatorActivity.class.getSimpleName();

    //TODO: add package prefix to these names as indicated at https://developer.android.com/reference/android/content/Intent#putExtra(java.lang.String,%20android.os.Parcelable)
    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    //public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String PARAM_USER_PASS = "USER_PASS";
    public final int REQ_SIGNUP = 1;
    protected static final String STATE_ALERT_MESSAGE = "state_dialog";

    protected AccountManager accountManager;

    private String authTokenType;
    private AsyncTask signinTask = null;
    private AlertDialog alertDialog = null;
    private String alertMessage = null;
    protected Library selected_library = null;
    protected Button forgotPasswordButton = null;

    protected void setContentViewImpl() {
        setContentView(R.layout.activity_login);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.initialize(this);
        setContentViewImpl();

        App.init(this);

        accountManager = AccountManager.get(getBaseContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        Analytics.log(TAG, "accountName=" + accountName);
        authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (authTokenType == null)
            authTokenType = Const.AUTHTOKEN_TYPE;
        Analytics.log(TAG, "authTokenType=" + authTokenType);

        TextView signInText = (TextView) findViewById(R.id.account_sign_in_text);
        signInText.setText(String.format(getString(R.string.ou_account_sign_in_message),
                AppState.getString(AppState.LIBRARY_NAME)));

        // Turn off suggestions for the accountName field.  Turning them off with setInputType worked on my phone
        // whereas using android:inputType="text|textNoSuggestions" in xml did not.
        TextView accountNameText = (TextView) findViewById(R.id.accountName);
        accountNameText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (accountName != null) {
            accountNameText.setText(accountName);
        }

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });

        try {
            forgotPasswordButton = (Button) findViewById(R.id.forgot_password_button);
        } catch (NoSuchFieldError e) {
        }
        if (forgotPasswordButton != null) {
            forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = getString(R.string.ou_library_url) + "/eg/opac/password_reset";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            });
        }

        if (savedInstanceState != null) {
            Analytics.log(TAG, "savedInstanceState=" + savedInstanceState);
            if (savedInstanceState.getString(STATE_ALERT_MESSAGE) != null) {
                showAlert(savedInstanceState.getString(STATE_ALERT_MESSAGE));
            }
        }
    }

    protected void initSelectedLibrary() {
        selected_library = new Library(getString(R.string.ou_library_url), getString(R.string.ou_library_name));
        Analytics.log(TAG, "initSelectedLibrary name=" + selected_library.getName()
                + " url=" + selected_library.getUrl());
    }

    @Override
    protected void onStart() {
        super.onStart();
        initSelectedLibrary();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (alertMessage != null) {
            outState.putString(STATE_ALERT_MESSAGE, alertMessage);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Analytics.log(TAG, "onActivityResult> requestCode=" + requestCode + " resultCode=" + resultCode);
        // The sign up activity returned that the user has successfully created
        // an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            onAuthSuccess(data);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void submit() {
        Analytics.log(TAG, "submit>");

        final String username = ((TextView) findViewById(R.id.accountName)).getText().toString();
        final String password = ((TextView) findViewById(R.id.accountPassword)).getText().toString();
        //final String account_type = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        signinTask = new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {

                Analytics.log(TAG, "signinTask> Start authenticating");

                String authtoken = null;
                String errorMessage = "Login failed";
                final String accountType = AuthenticatorActivity.this.getString(R.string.ou_account_type);
                Bundle data = new Bundle();
                try {
                    authtoken = EvergreenAuthenticator.signIn(selected_library.getUrl(), username, password);
                    Analytics.log(TAG, "signinTask> signIn returned " + Analytics.redactedString(authtoken));

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(PARAM_USER_PASS, password);
                    data.putString(Const.KEY_LIBRARY_NAME, selected_library.getName());
                    data.putString(Const.KEY_LIBRARY_URL, selected_library.getUrl());
                } catch (AuthenticationException e) {
                    if (e != null) errorMessage = e.getMessage();
                    //Analytics.logException(TAG, e);
                } catch (Exception e2) {
                    if (e2 != null) errorMessage = e2.getMessage();
                    Analytics.logException(TAG, e2);
                }
                if (authtoken == null)
                    data.putString(KEY_ERROR_MESSAGE, errorMessage);

                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                signinTask = null;
                Analytics.log(TAG, "signinTask.onPostExecute> intent=" + intent);
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Analytics.log(TAG, "signinTask.onPostExecute> error msg: " + intent.getStringExtra(KEY_ERROR_MESSAGE));
                    onAuthFailure(intent.getStringExtra(KEY_ERROR_MESSAGE));
                } else {
                    Analytics.log(TAG, "signinTask.onPostExecute> no error msg");
                    onAuthSuccess(intent);
                }
            }
        }.execute();
    }

    protected void onAuthFailure(String errorMessage) {
        showAlert(errorMessage);
    }

    protected void showAlert(String errorMessage) {
        if (isFinishing())
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        alertMessage = errorMessage;
        builder.setTitle("Login failed")
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog = null;
                        alertMessage = null;
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void onAuthSuccess(Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        String library_name = intent.getStringExtra(Const.KEY_LIBRARY_NAME);
        String library_url = intent.getStringExtra(Const.KEY_LIBRARY_URL);
        final Account account = new Account(accountName, accountType);
        Analytics.log(TAG, "onAuthSuccess> accountName=" + accountName
                        + " accountType=" + accountType
                        + " accountPassword=" + Analytics.redactedString(accountPassword)
                        + " library_name=" + library_name
                        + " library_url=" + library_url);

        //if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
        Analytics.log(TAG, "onAuthSuccess> addAccountExplicitly " + accountName);
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = authTokenType;

        // Create the account on the device
        Bundle userdata = null;
        if (!TextUtils.isEmpty(library_url)) {
            userdata = new Bundle();
            userdata.putString(Const.KEY_LIBRARY_NAME, library_name);
            userdata.putString(Const.KEY_LIBRARY_URL, library_url);
            Analytics.log(TAG, "onAuthSuccess> userdata, name=" + library_name + " url=" + library_url);
        }
        if (accountManager.addAccountExplicitly(account, accountPassword, userdata)) {
            Analytics.log(TAG, "onAuthSuccess> true, setAuthToken " + Analytics.redactedString(authtoken));
            // Not setting the auth token will cause another call to the server
            // to authenticate the user
            accountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            // Probably the account already existed, in which case update the password
            Analytics.log(TAG, "onAuthSuccess> false, setPassword, setUserData");
            accountManager.setPassword(account, accountPassword);
            accountManager.setUserData(account, Const.KEY_LIBRARY_NAME, library_name);
            accountManager.setUserData(account, Const.KEY_LIBRARY_URL, library_url);
            Analytics.log(TAG, "onAuthSuccess> now getUserData library_url=" + accountManager.getUserData(account, Const.KEY_LIBRARY_URL));
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
