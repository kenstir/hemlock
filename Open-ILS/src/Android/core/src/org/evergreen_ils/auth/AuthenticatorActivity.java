package org.evergreen_ils.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.evergreen_ils.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.globals.Utils;
import org.evergreen_ils.searchCatalog.Library;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.*;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private final String TAG = AuthenticatorActivity.class.getSimpleName();

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    //public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String PARAM_USER_PASS = "USER_PASS";
    private final int REQ_SIGNUP = 1;
    private static final String STATE_ALERT_MESSAGE = "state_dialog";

    private AccountManager accountManager;
    private Context context;
    private Spinner librarySpinner;
    private String authTokenType;
    private AsyncTask task = null;
    private AlertDialog alertDialog = null;
    private String alertMessage = null;
    Library selected_library = null;
    List<Library> libraries = new ArrayList<Library>();
    public String libraries_directory_json_url;

    private class FetchConsortiumsTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... params) {
            String url = params[0];
            String result = null;
            try {
                Log.d(TAG, "fetching "+url);
                result = Utils.getNetPageContent(url);
            } catch (Exception e) {
                Log.d(TAG, "error fetching", e);
            }
            return result;
        }

        protected void onPostExecute(String result) {
            Log.d(TAG, "results available: "+result);
            parseLibrariesJSON(result);
            ArrayList<String> l = new ArrayList<String>(libraries.size());
            for (Library library : libraries) {
                l.add(library.directory_name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, l);
            librarySpinner.setAdapter(adapter);
        }
    }

    // returns true if this is the generic app, which needs a library spinner etc.
    private boolean isGenericApp() {
        String library_url = getString(R.string.ou_library_url);
        return TextUtils.isEmpty(library_url);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AppPrefs.init(this);

        accountManager = AccountManager.get(getBaseContext());
        context = getApplicationContext();
        libraries_directory_json_url = getString(R.string.evergreen_libraries_url);

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        Log.d(TAG, "onCreate> accountName="+accountName);
        authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (authTokenType == null)
            authTokenType = Const.AUTHTOKEN_TYPE;
        Log.d(TAG, "onCreate> authTokenType="+authTokenType);

        if (isGenericApp()) {
            librarySpinner = (Spinner) findViewById(R.id.choose_library_spinner);
            librarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selected_library = libraries.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selected_library = null;
                }
            });
        } else {
            selected_library = new Library(getString(R.string.ou_library_url),
                    getString(R.string.ou_library_name), null);
        }

        TextView signInText = (TextView) findViewById(R.id.account_sign_in_text);
        signInText.setText(String.format(getString(R.string.ou_account_sign_in_message),
                AppPrefs.getString(AppPrefs.LIBRARY_NAME)));
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

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate> savedInstanceState="+savedInstanceState);
            if (savedInstanceState.getString(STATE_ALERT_MESSAGE) != null) {
                showAlert(savedInstanceState.getString(STATE_ALERT_MESSAGE));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onstart");
        if (isGenericApp()) {
            startTask();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onrestart");
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
                final String accountType = AuthenticatorActivity.this.getString(R.string.ou_account_type);
                Bundle data = new Bundle();
                try {
                    authtoken = EvergreenAuthenticator.signIn(selected_library.url, username, password);
                    Log.d(TAG, "task> signIn returned "+authtoken);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(PARAM_USER_PASS, password);
                    data.putString(Const.KEY_LIBRARY_URL, selected_library.url);
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
        String library_url = intent.getStringExtra(Const.KEY_LIBRARY_URL);
        final Account account = new Account(accountName, accountType);
        Log.d(TAG, "onAuthSuccess> accountName="+accountName);

        //if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
        Log.d(TAG, "onAuthSuccess> addAccountExplicitly "+accountName);
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = authTokenType;

        // Create the account on the device
        Bundle userdata = null;
        if (!TextUtils.isEmpty(library_url)) {
            userdata = new Bundle();
            userdata.putString(Const.KEY_LIBRARY_URL, library_url);
        }
        if (accountManager.addAccountExplicitly(account, accountPassword, userdata)) {
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

    private void startTask() {
        new FetchConsortiumsTask().execute(libraries_directory_json_url);
    }

    private void parseLibrariesJSON(String json) {
        libraries.clear();

        if (isDebuggable()) {
            //Library library = new Library("https://demo.evergreencatalog.com", "Evergreencatalog.com Demo", "0ut There, US  (evergreencatalog.com Example Consortium)");
            Library library = new Library("http://mlnc4.mvlcstaff.org", "MVLC Demo", "0ut There, US (MVLC Example Consortium)");// SSL not working
            libraries.add(library);
        }

        if (json != null) {
            List<Map<String,?>> l;
            try {
                l = (List<Map<String,?>>) new JSONReader(json).readArray();
            } catch (JSONException e) {
                Log.d(TAG, "failed parsing libraries array", e);
                return;
            }
            for (Map<String, ?> map : l) {
                String url = (String) map.get("url");
                String directory_name = (String) map.get("directory_name");
                String short_name = (String) map.get("short_name");
                Library library = new Library(url, short_name, directory_name);
                libraries.add(library);
            }

            Collections.sort(libraries, new Comparator<Library>() {
                @Override
                public int compare(Library a, Library b) {
                    return a.directory_name.compareTo(b.directory_name);
                }
            });

            for (int i = 0; i< libraries.size(); ++i) {
                Log.d(TAG, "c["+i+"]: "+ libraries.get(i).directory_name);
            }
        }
    }

    public boolean isDebuggable() {
        return ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }
}
