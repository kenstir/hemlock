/*
Copyright 2013 Udi Cohen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.evergreen_ils.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.evergreen_ils.R;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.CrashUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Udini
 * Date: 21/03/13
 * Time: 13:50
 */
public class TestAuthActivity extends Activity {
	
	private static final String STATE_DIALOG = "state_dialog";
	private static final String STATE_INVALIDATE = "state_invalidate";
    private static final String TAG = TestAuthActivity.class.getSimpleName();

    private AccountManager mAccountManager;
    private String mAccountType;
    private AlertDialog mAlertDialog;
    private boolean mInvalidate;
    private TextView mLastTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashUtils.onCreate(this);

        setContentView(R.layout.test_auth);
        mAccountManager = AccountManager.get(this);
        mAccountType = getString(org.evergreen_ils.R.string.ou_account_type);

        findViewById(R.id.btnAddAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewAccount(mAccountType,  Const.AUTHTOKEN_TYPE);
            }
        });

        findViewById(R.id.btnGetAuthToken).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPicker(Const.AUTHTOKEN_TYPE, false);
            }
        });

        findViewById(R.id.btnGetAuthTokenByFeatures).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTokenForAccountCreateIfNeeded(mAccountType, Const.AUTHTOKEN_TYPE);
            }
        });
        findViewById(R.id.btnInvalidateAuthToken).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPicker(Const.AUTHTOKEN_TYPE, true);
            }
        });
        
        mLastTextView = (TextView)findViewById(R.id.txtLastAccountName);

        if (savedInstanceState != null) {
        	boolean showDialog = savedInstanceState.getBoolean(STATE_DIALOG);
        	boolean invalidate = savedInstanceState.getBoolean(STATE_INVALIDATE);
        	if (showDialog) {
        		showAccountPicker(Const.AUTHTOKEN_TYPE, invalidate);
        	}
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if (mAlertDialog != null && mAlertDialog.isShowing()) {
    		outState.putBoolean(STATE_DIALOG, true);
    		outState.putBoolean(STATE_INVALIDATE, mInvalidate);
    	}
    }

    /**
     * Add new account to the account manager
     * @param accountType
     * @param authTokenType
     */
    private void addNewAccount(String accountType, String authTokenType) {
        //final AccountManagerFuture<Bundle> future = 
        mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    showMessage("Account "+account_name+" was created");
                    mLastTextView.setText(account_name);
                    Log.d(TAG, "AddNewAccount Bundle is " + bnd);

                } catch (Exception e) {
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
    private void showAccountPicker(final String authTokenType, final boolean invalidate) {
    	mInvalidate = invalidate;
        final Account availableAccounts[] = mAccountManager.getAccountsByType(mAccountType);

        if (availableAccounts.length == 0) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show();
            addNewAccount(mAccountType, Const.AUTHTOKEN_TYPE);
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
                String library_name = mAccountManager.getUserData(availableAccounts[i], Const.KEY_LIBRARY_NAME);
                String library_url = mAccountManager.getUserData(availableAccounts[i], Const.KEY_LIBRARY_URL);
                Log.d(TAG, "name:"+name[i]+" library_name:"+library_name+" url:"+library_url);
            }

            // Account picker
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mAlertDialog = builder.setTitle("Pick Account").setItems(name, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(invalidate)
                        invalidateAuthToken(availableAccounts[which], authTokenType);
                    else
                        getExistingAccountAuthToken(availableAccounts[which], authTokenType);
                }
            }).create();

            mAlertDialog.show();
        }
    }

    /**
     * Get the auth token for an existing account on the AccountManager
     * @param account
     * @param authTokenType
     */
    private void getExistingAccountAuthToken(final Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();

                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    showMessage((authtoken != null) ? "SUCCESS with "+account_name+"\ntoken: " + authtoken : "FAIL");
                    Log.d(TAG, "GetToken Bundle is " + bnd);
                    //mLastTextView.setText(account.name);//todo: wrong thread here
                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }).start();
    }

    private void reuseExistingAccountAuthToken(final String account_name, String authTokenType) {
        final Account availableAccounts[] = mAccountManager.getAccountsByType(mAccountType);
        final Account account = null;
        for (int i = 0; i < availableAccounts.length; i++) {
            if (account_name.equals(availableAccounts[i].name)) {
                Toast.makeText(this, "Reusing last account "+account_name, Toast.LENGTH_SHORT);
                getExistingAccountAuthToken(availableAccounts[i], authTokenType);
            }
        }
    }
    
    /**
     * Invalidates the auth token for the account
     * @param account
     * @param authTokenType
     */
    private void invalidateAuthToken(final Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null,null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();

                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    mAccountManager.invalidateAuthToken(account.type, authtoken);
                    showMessage(account.name + " invalidated");
                    mLastTextView.setText("no account");
                } catch (Exception e) {
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
    private void getTokenForAccountCreateIfNeeded(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(accountType, authTokenType, null, this, null, null,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bnd = null;
                        try {
                            bnd = future.getResult();
                            final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                            final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                            final String library_name = bnd.getString(Const.KEY_LIBRARY_NAME);
                            final String library_url = bnd.getString(Const.KEY_LIBRARY_URL);
                            showMessage((authtoken != null) ? "SUCCESS with "+account_name+"\nlibrary_url: " + library_url: "FAIL");
                            Log.d(TAG, "GetTokenForAccount Bundle is " + bnd);
                            mLastTextView.setText(account_name);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showMessage(e.getMessage());
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
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                mLastTextView.setText(msg);
            }
        });
    }
}
