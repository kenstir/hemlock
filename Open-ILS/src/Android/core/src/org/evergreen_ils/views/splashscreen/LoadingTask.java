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
package org.evergreen_ils.views.splashscreen;

import android.text.TextUtils;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.globals.GlobalConfigs;

import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import org.evergreen_ils.searchCatalog.Library;

/** This is basically the same as an AsyncTask<String,String,String>, except that it uses
 * a Thread.  Starting with HONEYCOMB, tasks are executed on a single thread and the 2nd
 * AsyncTask doesn't start until the first finishes.
 * 
 * @author kenstir
 *
 */
public class LoadingTask {
    private final String TAG = LoadingTask.class.getSimpleName();

    public static final String TASK_OK = "OK";

    public interface LoadingTaskListener {
        void onPreExecute();
        void onProgressUpdate(String value);
        void onPostExecute(String result);
    }

    private final LoadingTaskListener mListener;
    private Activity mCallingActivity;

    public LoadingTask(LoadingTaskListener listener, Activity callingActivity) {
        this.mListener = listener;
        this.mCallingActivity = callingActivity;
    }

    public void execute() {
        Log.d(TAG, "execute>");
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = doInBackground();
                mCallingActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onPostExecute(result);
                    }
                });
            }
        }, TAG);
        onPreExecute();
        t.start();
    }

    protected void publishProgress(final String progress) {
        mCallingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onProgressUpdate(progress);
            }
        });
    }

    //TODO: share some of this code with ScheduledIntentService.onHandleIntent
    protected String doInBackground() {
        final String tag ="doInBackground> ";
        final String accountType = mCallingActivity.getString(R.string.ou_account_type);
        try {
            Log.d(TAG, tag+"Signing in");
            publishProgress("Signing in");

            Bundle bnd = AccountUtils.getAuthToken(mCallingActivity);
            String auth_token = bnd.getString(AccountManager.KEY_AUTHTOKEN);
            String account_name = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
            if (TextUtils.isEmpty(auth_token) || TextUtils.isEmpty(account_name))
                return "no account";

            Library library = AccountUtils.getLibraryForAccount(mCallingActivity, account_name, accountType);
            AppPrefs.setString(AppPrefs.LIBRARY_NAME, library.name);
            AppPrefs.setString(AppPrefs.LIBRARY_URL, library.url);

            Log.d(TAG, tag+"Loading resources from "+library.url);
            publishProgress("Loading resources");
            GlobalConfigs.getGlobalConfigs(mCallingActivity, library.url);

            Log.d(TAG, tag+"Starting session");
            publishProgress("Starting session");
            AccountAccess ac = AccountAccess.getAccountAccess();

            // auth token zen: try once and if it fails, invalidate the token and try again
            boolean haveSession = false;
            try {
                haveSession = ac.retrieveSession(auth_token);
            } catch (SessionNotFoundException e) {
                try {
                    haveSession = ac.reauthenticate(mCallingActivity, account_name);
                } catch (SessionNotFoundException e2) {
                }
            }
            if (!haveSession)
                return "no session";

            Log.d(TAG, tag+"Retrieving bookbags");
            publishProgress("Retrieving bookbags");
            ac.retrieveBookbags();

            return TASK_OK;
        } catch (Exception e) {
            Log.d(TAG, tag+"Caught exception", e);
            String s = e.getMessage();
            if (s == null) s = "Cancelled";
            Log.d(TAG, tag+"returning "+s);
            return s;
        }
    }

    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute> ");
        mListener.onPreExecute();
    }

    protected void onProgressUpdate(String s) {
        Log.d(TAG, "onProgressUpdate> "+s);
        mListener.onProgressUpdate(s);
    }
    
    protected void onPostExecute(String result) {
        Log.d(TAG, "onPostExecute> "+result);
        mListener.onPostExecute(result);
    }
}
