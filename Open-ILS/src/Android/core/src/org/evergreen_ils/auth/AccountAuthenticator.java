package org.evergreen_ils.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    
    private final String TAG = AccountAuthenticator.class.getName();
    private Context context;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "addaccount "+accountType+" "+authTokenType);
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        // setting ARG_IS_ADDING_NEW_ACCOUNT here does not work, because this is not the
        // same Intent as the one in AuthenticatorActivity.finishLogin
        //intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        Bundle result = new Bundle();
        result.putParcelable(AccountManager.KEY_INTENT, intent);
        return result;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "getAuthToken> "+account.name);

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Const.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        final AccountManager am = AccountManager.get(context);
        String authToken = am.peekAuthToken(account, authTokenType);
        Log.d(TAG, "getAuthToken> peekAuthToken returned " + authToken);
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                try {
                    Log.d(TAG, "getAuthToken> attempting to sign in with existing password");
                    authToken = EvergreenAuthenticator.signIn(context, account.name, password);
                    Log.d(TAG, "getAuthToken> signIn returned token "+authToken);
                } catch (AuthenticationException e) {
                    Log.d(TAG, "getAuthToken> caught exception", e);
                    Log.d(TAG, "getAuthToken> caught exception "+e.getMessage());
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                    return result;
                } catch (Exception e2) {
                    Log.d(TAG, "getAuthToken> caught other Exception");
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, "Sign in failed");
                    return result;
                }
            }
        }

        // If we get an authToken - we return it
        Log.d(TAG, "getAuthToken> token "+authToken);
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        Log.d(TAG, "getAuthToken> creating intent to display AuthenticatorActivity");
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return Const.AUTHTOKEN_TYPE_LABEL;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Log.d(TAG, "hasFeatures "+account.name+" features "+features);
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.d(TAG, "editProperties "+accountType);
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "confirmCredentials "+account.name);
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "updateCredentials "+account.name);
        return null;
    }
}