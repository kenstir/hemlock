/*
 * Copyright (c) 2025 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.hemlock.android;

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import net.kenstir.hemlock.R;
import net.kenstir.hemlock.android.accounts.Const;
import org.evergreen_ils.data.Library;

import java.io.IOException;

import static org.evergreen_ils.utils.StringUtils.safeString;

public class AccountUtils {

    public static Library getLibraryForAccount(Context context, String account_name, String account_type) {
        final AccountManager am = AccountManager.get(context);
        Account account = new Account(account_name, account_type);

        // For custom apps, library_url should come from the resources.
        // For the generic Hemlock app, it is stored as user data in the AccountManager.
        String library_url = context.getString(R.string.ou_library_url);
        if (TextUtils.isEmpty(library_url)) {
            library_url = am.getUserData(account, Const.KEY_LIBRARY_URL);
        }

        String library_name = am.getUserData(account, Const.KEY_LIBRARY_NAME);
        if (TextUtils.isEmpty(library_name)) {
            library_name = context.getString(R.string.ou_library_name);
        }

        return new Library(library_url, library_name);
    }

    public static Library getLibraryForAccount(Context context, Account account) {
        return getLibraryForAccount(context, account.name, account.type);
    }

    public static void invalidateAuthToken(Context context, String auth_token) {
        Log.i(Const.AUTH_TAG, "invalidateAuthToken "+auth_token);
        if (TextUtils.isEmpty(auth_token))
            return;
        final AccountManager am = AccountManager.get(context);
        final String accountType = context.getString(R.string.ou_account_type);
        am.invalidateAuthToken(accountType, auth_token);
    }

    public static void clearPassword(Context context, String account_name) {
        Log.i(Const.AUTH_TAG, "clearPassword "+account_name);
        if (TextUtils.isEmpty(account_name))
            return;
        final AccountManager am = AccountManager.get(context);
        final String accountType = context.getString(R.string.ou_account_type);
        final Account account = new Account(account_name, accountType);
        am.clearPassword(account);
    }

    public static String getPassword(Context context, String account_name) {
        if (TextUtils.isEmpty(account_name))
            return "";
        final AccountManager am = AccountManager.get(context);
        final String accountType = context.getString(R.string.ou_account_type);
        final Account account = new Account(account_name, accountType);
        return safeString(am.getPassword(account));
    }

    public static String getAuthTokenForAccount(Activity activity, String account_name) throws AuthenticatorException, OperationCanceledException, IOException {
        Log.i(Const.AUTH_TAG, "getAuthTokenForAccount "+account_name);
        if (runningOnUIThread() || TextUtils.isEmpty(account_name)) {
            Log.i(Const.AUTH_TAG, "getAuthTokenForAccount returns null");
            return null;
        }
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        final Account account = new Account(account_name, accountType);
        Bundle b = am.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, activity, null, null).getResult();
        final String auth_token = b.getString(AccountManager.KEY_AUTHTOKEN);
        Log.i(Const.AUTH_TAG, "getAuthTokenForAccount " + account_name + " returns " + auth_token);
        return auth_token;
    }

    public static AccountManagerFuture<Bundle> getAuthTokenForAccountFuture(Activity activity, String account_name) throws AuthenticatorException, OperationCanceledException, IOException {
        Log.i(Const.AUTH_TAG, "getAuthTokenForAccountFuture "+account_name);
        if (TextUtils.isEmpty(account_name)) {
            return null;
        }
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        final Account account = new Account(account_name, accountType);
        return am.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, activity, null, null);
    }

    public static Bundle getAuthToken(Activity activity) throws AuthenticatorException, OperationCanceledException, IOException {
        Log.i(Const.AUTH_TAG, "getAuthToken");
        if (runningOnUIThread())
            return new Bundle();
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        AccountManagerFuture<Bundle> future = am.getAuthTokenByFeatures(accountType, Const.AUTHTOKEN_TYPE, null, activity, null, null, null, null);
        Bundle bnd = future.getResult();
        Log.i(Const.AUTH_TAG, "getAuthToken returns "+bnd);
        return bnd;
    }

    public static AccountManagerFuture<Bundle> getAuthTokenFuture(Activity activity) throws AuthenticatorException, OperationCanceledException, IOException {
        Log.i(Const.AUTH_TAG, "getAuthTokenFuture");
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        return am.getAuthTokenByFeatures(accountType, Const.AUTHTOKEN_TYPE, null, activity, null, null, null, null);
    }

    public static Account[] getAccountsByType(Activity activity) {
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        final Account[] availableAccounts = am.getAccountsByType(accountType);
        return availableAccounts;
    }

    public static boolean haveMoreThanOneAccount(Activity activity) {
        Account[] availableAccounts = getAccountsByType(activity);
        return availableAccounts.length > 1;
    }

    public static void addAccount(final Activity activity, final Runnable runnable) {
        Log.d(Const.AUTH_TAG, "addAccount");
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        am.addAccount(accountType, Const.AUTHTOKEN_TYPE, null, null, activity, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    Log.d(Const.AUTH_TAG, "added account bnd=" + bnd);
                    activity.runOnUiThread(runnable);
                } catch (Exception e) {
                    Log.d(Const.AUTH_TAG, "failed to add account", e);
                }
            }
        }, null);
    }

    public static boolean runningOnUIThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }
}
