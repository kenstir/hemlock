/*
 * Copyright (C) 2017 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.utils.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.views.BarcodeActivity;
import org.evergreen_ils.accountAccess.bookbags.BookBagActivity;
import org.evergreen_ils.accountAccess.checkout.CheckoutsActivity;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsActivity;
import org.evergreen_ils.android.App;
import org.evergreen_ils.searchCatalog.SearchActivity;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.views.MainActivity;
import org.evergreen_ils.views.MenuProvider;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.net.URLEncoder;

import static org.evergreen_ils.android.App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT;

/* Activity base class to handle common behaviours like the navigation drawer */
public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static String TAG = BaseActivity.class.getSimpleName();

    protected Toolbar mToolbar;
    protected MenuProvider mMenuItemHandler = null;
    protected boolean mRestarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            mRestarting = true;
            return;
        }
        mRestarting = false;

        Analytics.initialize(this);
        App.init(this);
        applyNightMode();

        initMenuProvider();
        if (mMenuItemHandler != null)
            mMenuItemHandler.onCreate(this);
    }

    public Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = ActionBarUtils.initActionBarForActivity(this, null, true);
        }
        return mToolbar;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getToolbar();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            View navHeader = navigationView.getHeaderView(0);
            if (navHeader != null)
                navHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onNavigationAction(v.getId());
                    }
                });
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    void initMenuProvider() {
        mMenuItemHandler = MenuProvider.create(getString(R.string.ou_menu_provider));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_feedback) {
            Analytics.logEvent("Feedback: Open");
            String url = getFeedbackUrl();
            if (!TextUtils.isEmpty(url)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        return onNavigationAction(id);
    }

    protected boolean onNavigationAction(int id) {
        boolean ret = true;
        if (id == R.id.nav_header) {
            Analytics.logEvent("Home: Open", "via", "nav_drawer");
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else if (id == R.id.main_btn_search) {
            Analytics.logEvent("Search: Open", "via", "nav_drawer");
            startActivity(new Intent(this, SearchActivity.class));
        } else if (id == R.id.account_btn_check_out) {
            Analytics.logEvent("Checkouts: Open", "via", "nav_drawer");
            startActivity(new Intent(this, CheckoutsActivity.class));
        } else if (id == R.id.account_btn_holds) {
            Analytics.logEvent("Holds: Open", "via", "nav_drawer");
            startActivity(new Intent(this, HoldsActivity.class));
        } else if (id == R.id.account_btn_fines) {
            Analytics.logEvent("Fines: Open", "via", "nav_drawer");
            startActivity(new Intent(this, FinesActivity.class));
        } else if (id == R.id.main_my_lists_button) {
            Analytics.logEvent("Lists: Open", "via", "nav_drawer");
            startActivity(new Intent(this, BookBagActivity.class));
        } else if (id == R.id.btn_barcode) {
            Analytics.logEvent("Barcode: Open", "via", "nav_drawer");
            // generating via Intent only works if zxing barcode app is installed
//            Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
//            intent.putExtra("ENCODE_FORMAT", "CODABAR");
//            intent.putExtra("ENCODE_DATA", "12345678901234");
//            startActivity(intent);
            startActivity(new Intent(this, BarcodeActivity.class));
        } else if (mMenuItemHandler != null) {
            ret = mMenuItemHandler.onItemSelected(this, id, "nav_drawer");
        } else {
            ret = false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return ret;
    }

    public boolean handleMenuAction(int id) {
        if (id == R.id.action_switch_account) {
            Analytics.logEvent("Account: Switch Account", "via", "options_menu");
            SplashActivity.restartApp(this);
            return true;
        } else if (id == R.id.action_add_account) {
            Analytics.logEvent("Account: Add Account", "via", "options_menu");
            invalidateOptionsMenu();
            AccountUtils.addAccount(this, new Runnable() {
                @Override
                public void run() {
                    SplashActivity.restartApp(BaseActivity.this);
                }
            });
            return true;
        } else if (id == R.id.action_logout) {
            Analytics.logEvent("Account: Logout", "via", "options_menu");
            AccountAccess.getInstance().logout(this);
            SplashActivity.restartApp(this);
            return true;
//        } else if (id == R.id.action_feedback) {
//            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getFeedbackUrl())));
//            return true;
        } else if (id == R.id.action_messages) {
            Analytics.logEvent("Messages: Open", "via", "options_menu");
            String username = AccountAccess.getInstance().getUserName();
            String password = AccountUtils.getPassword(this, username);
            String path = "/eg/opac/login"
                    + "?redirect_to=" + URLEncoder.encode("/eg/opac/myopac/messages");
            if (!TextUtils.isEmpty(username))
                path = path + "&username=" + URLEncoder.encode(username);
            if (!TextUtils.isEmpty(password))
                path = path + "&password=" + URLEncoder.encode(password);
            String url = EvergreenServer.getInstance().getUrl(path);
            startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT);
            return true;
        } else if (id == R.id.action_dark_mode) {
            saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            // return false or else the menu view will be leaked when the activity is restarted
            return false;
        } else if (id == R.id.action_light_mode) {
            saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            // return false or else the menu view will be leaked when the activity is restarted
            return false;
        }
        return false;
    }

    protected void saveAndApplyNightMode(int nightMode) {
        AppState.setInt(AppState.NIGHT_MODE, nightMode);
        Log.d(TAG,"saveAndApplyNightMode:"+nightMode);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    protected void applyNightMode() {
        int nightMode = AppState.getInt(AppState.NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES);
        Log.d(TAG,"applyNightMode:"+nightMode);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    @SuppressLint("StringFormatInvalid")
    protected String getFeedbackUrl() {
        String urlFormat = getString(R.string.ou_feedback_url);
        if (urlFormat.isEmpty())
            return urlFormat;
        return String.format(urlFormat, getAppVersionCode(this));
    }

    public static String getAppVersionCode(Context context) {
        String version = "";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = String.format("%d", pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Log", "caught", e);
        }
        return version;
    }
}
