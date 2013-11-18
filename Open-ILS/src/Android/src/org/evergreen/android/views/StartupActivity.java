package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.searchCatalog.SearchCatalogListView;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class StartupActivity extends Activity {

    private TextView mLoginStatusMessageView;
    private StartupTask mStartupTask = null;
    private String mAlertMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        
        //TODO
        /*
        last_username = getPref();
        if (last_username) getAuthToken()
         */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.startup, menu);
        return true;
    }
    */

    private void startNextActivity() {
        Intent intent = new Intent(this, SearchCatalogListView.class);
        startActivity(intent);
        finish();
    }

    public void downloadResources() {
        if (mStartupTask != null) {
            return;
        }
        
        // blah blah blah
        // mDownloadTask = new LoginTask();
    }

    public class StartupTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // TODO
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            return true;
        }
        
        @Override
        protected void onProgressUpdate(Void... params) {
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mStartupTask = null;
            if (success) {
                startNextActivity();
            }
        }

        @Override
        protected void onCancelled() {
            mStartupTask = null;
        }
    }
}
