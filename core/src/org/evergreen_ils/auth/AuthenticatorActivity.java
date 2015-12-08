package org.evergreen_ils.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
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
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.globals.Library;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.*;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private final static String TAG = AuthenticatorActivity.class.getName();

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
    private long start_ms;

    private void handleLibrariesJSON(String result) {
        // parse the response
        parseLibrariesJSON(result);

        // if the user has any existing accounts, then we can select a reasonable default library
        Library default_library = null;
        Location last_location = null;
        Account[] existing_accounts = AccountUtils.getAccountsByType(AuthenticatorActivity.this);
        Log.d(Const.AUTH_TAG, "there are " + existing_accounts.length + " existing accounts");
        if (existing_accounts.length > 0) {
            default_library = AccountUtils.getLibraryForAccount(AuthenticatorActivity.this, existing_accounts[0]);
            Log.d(Const.AUTH_TAG, "default_library=" + default_library);
        } else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) last_location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // Build a List<String> for use in the spinner adapter
        // While we're at it choose a default library; first by prior account, second by proximity
        Integer default_library_index = null;
        float min_distance = Float.MAX_VALUE;
        ArrayList<String> l = new ArrayList<String>(libraries.size());
        for (Library library : libraries) {
            if (default_library != null && TextUtils.equals(default_library.url, library.url)) {
                default_library_index = l.size();
            } else if (last_location != null && library.location != null) {
                float distance = last_location.distanceTo(library.location);
                if (distance < min_distance) {
                    default_library_index = l.size();
                    min_distance = distance;
                }
            }
            l.add(library.directory_name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, l);
        librarySpinner.setAdapter(adapter);
        if (default_library_index != null) {
            librarySpinner.setSelection(default_library_index);
        }
    }

    private void startTask() {
        start_ms = System.currentTimeMillis();
        RequestQueue q = VolleyWrangler.getInstance(this).getRequestQueue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, libraries_directory_json_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        long duration_ms = System.currentTimeMillis() - start_ms;
                        Log.d(TAG, "volley fetch took " + duration_ms + "ms");
                        handleLibrariesJSON(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showAlert(error.getMessage());
                    }
                });
        q.add(stringRequest);
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
        Log.d(TAG, "onCreate> accountName=" + accountName);
        authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (authTokenType == null)
            authTokenType = Const.AUTHTOKEN_TYPE;
        Log.d(TAG, "onCreate> authTokenType=" + authTokenType);

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
            selected_library = new Library(getString(R.string.ou_library_url), getString(R.string.ou_library_name));
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
            Log.d(TAG, "onCreate> savedInstanceState=" + savedInstanceState);
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
        Log.d(TAG, "onActivityResult> requestCode=" + requestCode + " resultCode=" + resultCode);
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
                    Log.d(TAG, "task> signIn returned " + authtoken);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(PARAM_USER_PASS, password);
                    data.putString(Const.KEY_LIBRARY_NAME, selected_library.name);
                    data.putString(Const.KEY_LIBRARY_URL, selected_library.url);
                } catch (AuthenticationException e) {
                    if (e != null) errorMessage = e.getMessage();
                    Log.d(TAG, "task> signIn caught auth exception " + errorMessage);
                } catch (Exception e2) {
                    if (e2 != null) errorMessage = e2.getMessage();
                    Log.d(TAG, "task> signIn caught other exception " + errorMessage);
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
                Log.d(TAG, "task.onPostExecute> intent=" + intent);
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Log.d(TAG, "task.onPostExecute> error msg: " + intent.getStringExtra(KEY_ERROR_MESSAGE));
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
        String library_name = intent.getStringExtra(Const.KEY_LIBRARY_NAME);
        String library_url = intent.getStringExtra(Const.KEY_LIBRARY_URL);
        final Account account = new Account(accountName, accountType);
        Log.d(TAG, "onAuthSuccess> accountName=" + accountName);

        //if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
        Log.d(TAG, "onAuthSuccess> addAccountExplicitly " + accountName);
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = authTokenType;

        // Create the account on the device
        Bundle userdata = null;
        if (!TextUtils.isEmpty(library_url)) {
            userdata = new Bundle();
            userdata.putString(Const.KEY_LIBRARY_NAME, library_name);
            userdata.putString(Const.KEY_LIBRARY_URL, library_url);
        }
        if (accountManager.addAccountExplicitly(account, accountPassword, userdata)) {
            Log.d(TAG, "onAuthSuccess> true, setAuthToken " + authtoken);
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

    private void parseLibrariesJSON(String json) {
        libraries.clear();

        if (isDebuggable(this)) {
            Library library = new Library("https://demo.evergreencatalog.com", "evergreencatalog.com Demo", "0ut There, US  (evergreencatalog.com Demo)", null);
            //Library library = new Library("http://mlnc4.mvlcstaff.org"), "MVLC Demo", "0ut There, US (MVLC Demo)", null);// Android does not like this cert
            libraries.add(library);
        }

        if (json != null) {
            List<Map<String, ?>> l;
            try {
                l = (List<Map<String, ?>>) new JSONReader(json).readArray();
            } catch (JSONException e) {
                Log.d(TAG, "failed parsing libraries array", e);
                return;
            }
            for (Map<String, ?> map : l) {
                String url = (String) map.get("url");
                String directory_name = (String) map.get("directory_name");
                String short_name = (String) map.get("short_name");
                Double latitude = (Double) map.get("latitude");
                Double longitude= (Double) map.get("longitude");
                Location location = new Location("");
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                Library library = new Library(url, short_name, directory_name, location);
                libraries.add(library);
            }

            Collections.sort(libraries, new Comparator<Library>() {
                @Override
                public int compare(Library a, Library b) {
                    return a.directory_name.compareTo(b.directory_name);
                }
            });

            for (int i = 0; i < libraries.size(); ++i) {
                Log.d(TAG, "c[" + i + "]: " + libraries.get(i).directory_name);
            }
        }
    }

    public static boolean isDebuggable(Context context) {
        return (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }
}
