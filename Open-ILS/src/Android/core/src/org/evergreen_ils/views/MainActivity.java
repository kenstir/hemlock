package org.evergreen_ils.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsListView;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.SearchCatalogListView;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

/**
 * Created by kenstir on 12/28/13.
 */
public class MainActivity extends ActionBarActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private GlobalConfigs globalConfigs;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.activity_main);
        ActionBarUtils.initActionBarForActivity(this, true);

        // singleton initialize necessary IDL and Org data
        globalConfigs = GlobalConfigs.getInstance(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        String url = getString(R.string.ou_feedback_url);
        if (TextUtils.isEmpty(url))
            menu.removeItem(R.id.action_feedback);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.getItem(0).setEnabled(AccountUtils.haveMoreThanOneAccount(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_switch_account) {
            SplashActivity.restartApp(this);
            return true;
        } else if (id == R.id.action_add_account) {
            invalidateOptionsMenu();
            AccountUtils.addAccount(this, new Runnable() {
                @Override
                public void run() {
                    SplashActivity.restartApp(MainActivity.this);
                }
            });
            Log.i(Const.AUTH_TAG, "after addAccount");
            return true;
        } else if (id == R.id.action_feedback) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.ou_feedback_url)));
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.account_btn_check_out) {
            startActivity(new Intent(getApplicationContext(), ItemsCheckOutListView.class));

        } else if (id == R.id.account_btn_holds) {
            startActivity(new Intent(getApplicationContext(), HoldsListView.class));

        } else if (id == R.id.account_btn_fines) {
            startActivity(new Intent(getApplicationContext(), FinesActivity.class));

            /*
        } else if (id == R.id.account_btn_book_bags) {
            startActivity(new Intent(getApplicationContext(), BookbagsListView.class));
            */

        } else if (id == R.id.main_btn_search) {
            startActivity(new Intent(getApplicationContext(), SearchCatalogListView.class));
        }
    }
}
