package org.evergreen_ils.utils.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.bookbags.BookBagListView;
import org.evergreen_ils.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsListView;
import org.evergreen_ils.searchCatalog.SearchActivity;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.evergreen_ils.views.MenuProvider;
import org.evergreen_ils.views.splashscreen.SplashActivity;

/* Activity base class to handle common behaviours like the navigation drawer */
public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected Toolbar mToolbar;
    protected MenuProvider mMenuItemHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }
        SearchFormat.init(this);
        AppState.init(this);
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null)
            navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
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
        /*
        if (id == R.id.action_settings) {
            // handle it
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        boolean ret = true;
        int id = item.getItemId();
        if (id == R.id.main_btn_search) {
            startActivity(new Intent(this, SearchActivity.class));
        } else if (id == R.id.account_btn_check_out) {
            startActivity(new Intent(this, ItemsCheckOutListView.class));
        } else if (id == R.id.account_btn_holds) {
            startActivity(new Intent(this, HoldsListView.class));
        } else if (id == R.id.account_btn_fines) {
            startActivity(new Intent(this, FinesActivity.class));
        } else if (id == R.id.main_my_lists_button) {
            startActivity(new Intent(this, BookBagListView.class));
        } else if (mMenuItemHandler != null) {
            ret = mMenuItemHandler.onItemSelected(this, id);
        } else {
            ret = false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return ret;
    }
}
