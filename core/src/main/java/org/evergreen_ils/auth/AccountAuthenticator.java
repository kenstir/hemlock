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
import org.evergreen_ils.R;
import org.evergreen_ils.android.Analytics;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    
    private final static String TAG = AccountAuthenticator.class.getSimpleName();
    private Context context;
    private Class authenticatorActivity;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;

        // Choose the right AuthenticatorActivity.  A custom app does not require the library spinner.
        String library_url = context.getString(R.string.ou_library_url);
        if (TextUtils.isEmpty(library_url)) {
            this.authenticatorActivity = GenericAuthenticatorActivity.class;
        } else {
            this.authenticatorActivity = AuthenticatorActivity.class;
        }
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Analytics.log(TAG, "addaccount "+accountType+" "+authTokenType);
        final Intent intent = new Intent(context, authenticatorActivity);
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
        Analytics.log(TAG, "getAuthToken> "+account.name);

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Const.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        final AccountManager am = AccountManager.get(context);
        String library_name = am.getUserData(account, Const.KEY_LIBRARY_NAME);
        String library_url = am.getUserData(account, Const.KEY_LIBRARY_URL);
        Analytics.log(TAG, "getAuthToken> library_name=" + library_name + " library_url=" + library_url);
        if (library_name == null) {
            // workaround issue #24 - not sure how it happened
            library_name = context.getString(R.string.ou_library_name);
        }
        if (library_url == null) {
            // workaround issue #24 - not sure how it happened
            library_url = context.getString(R.string.ou_library_url);
        }

        String authToken = am.peekAuthToken(account, authTokenType);
        Analytics.log(TAG, "getAuthToken> peekAuthToken returned " + authToken);
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                try {
                    Analytics.log(TAG, "getAuthToken> attempting to sign in with existing password");
                    authToken = EvergreenAuthenticator.signIn(library_url, account.name, password);
                } catch (AuthenticationException e) {
                    Analytics.logException(e);
                    am.clearPassword(account);
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                    return result;
                } catch (Exception e2) {
                    Analytics.logException(e2);
                    am.clearPassword(account);
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ERROR_MESSAGE, "Sign in failed");
                    return result;
                }
            }
        }

        // If we get an authToken - we return it
        Analytics.log(TAG, "getAuthToken> token "+Analytics.redactedString(authToken));
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            result.putString(Const.KEY_LIBRARY_NAME, library_name);
            result.putString(Const.KEY_LIBRARY_URL, library_url);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        Analytics.log(TAG, "getAuthToken> creating intent to display AuthenticatorActivity");
        final Intent intent = new Intent(context, authenticatorActivity);
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
        Analytics.log(TAG, "hasFeatures "+account.name+" features "+features);
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Analytics.log(TAG, "editProperties "+accountType);
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Analytics.log(TAG, "confirmCredentials "+account.name);
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Analytics.log(TAG, "updateCredentials "+account.name);
        return null;
    }
}
