package org.evergreen_ils.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.bookbags.BookbagsListView;
import org.evergreen_ils.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsListView;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.searchCatalog.AdvancedSearchActivity;
import org.evergreen_ils.searchCatalog.SearchCatalogListView;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

/**
 * Created by kenstir on 12/28/13.
 */
public class MainActivity extends ActionBarActivity {

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
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_switch_account) {
            SplashActivity.restartApp(this);
            return true;
        } else if (id == R.id.action_add_account) {
            AccountUtils.addAccount(this, new Runnable() {
                @Override
                public void run() {
                    SplashActivity.restartApp(MainActivity.this);
                }
            });
            Log.i(Const.AUTH_TAG, "after addAccount");
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
