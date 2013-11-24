/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen.android.views.splashscreen;

import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen_ils.auth.Const;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingTask extends AsyncTask<String, String, String> {
    private String TAG = "LoadingTask";
    
    public static final String TASK_OK = "OK";

    public interface LoadingTaskListener {
        void onPreExecute();
        void onProgressUpdate(String value);
        void onPostExecute(String result);
    }

    // This is the listener that will be told when this task is finished
    private final LoadingTaskListener mListener;
    private Activity mCallingActivity;
    private AccountManager mAccountManager;

    public LoadingTask(LoadingTaskListener listener, Activity callingActivity) {
        this.mListener = listener;
        this.mCallingActivity = callingActivity;
        mAccountManager = AccountManager.get(callingActivity);
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mListener.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            Log.d(TAG, "Loading resources");
            publishProgress("Loading resources");
            GlobalConfigs gl = GlobalConfigs.getGlobalConfigs(mCallingActivity);

            Log.d(TAG, "Signing in");
            publishProgress("Signing in");
            final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(Const.ACCOUNT_TYPE, Const.AUTHTOKEN_TYPE, null, mCallingActivity, null, null, null, null);
            Bundle bnd = future.getResult();
            Log.d(TAG, "bnd="+bnd);
            final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
            final String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
            Log.d(TAG, "account_name="+account_name+" token="+authtoken);
            //onSuccessfulLogin(account_name, authtoken);
            if (account_name == null)
                return "no account";

            Log.d(TAG, "Starting session");
            publishProgress("Starting session");
            AccountAccess ac = AccountAccess.getAccountAccess(GlobalConfigs.httpAddress);
            if (!ac.initSession())
                return "no session";

            Log.d(TAG, "Retrieving bookbags");
            publishProgress("Retrieving bookbags");
            ac.retrieveBookbags();

            return TASK_OK;
        } catch (Exception e) {
            Log.d(TAG, "Caught exception", e);
            return e.getMessage();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mListener.onProgressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        mListener.onPostExecute(result);
    }
}