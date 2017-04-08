/*
 * Copyright (C) 2015 Kenneth H. Cox
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

package org.evergreen_ils.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.bookbags.BookBagListView;
import org.evergreen_ils.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsListView;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.billing.BillingDataProvider;
import org.evergreen_ils.billing.BillingHelper;
import org.evergreen_ils.billing.IabResult;
import org.evergreen_ils.searchCatalog.SearchActivity;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.evergreen_ils.system.EvergreenServerLoader;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.AppState;
import org.evergreen_ils.utils.ui.BaseActivity;
import org.evergreen_ils.views.splashscreen.SplashActivity;

/**
 * Created by kenstir on 12/28/13.
 */
public class MainActivity extends BaseActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    protected MenuProvider menuProvider = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initBillingProvider();
        initMenuProvider();
        if (menuProvider != null)
            menuProvider.onCreate(this);
        EvergreenServerLoader.fetchOrgSettings(this);
        EvergreenServerLoader.fetchSMSCarriers(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important to dispose of the helper
        BillingHelper.disposeIabHelper();
    }

    void initBillingProvider() {
        // get the public key
        BillingDataProvider provider = BillingDataProvider.create(getString(R.string.ou_billing_data_provider));
        String base64EncodedPublicKey = (provider != null) ? provider.getPublicKey() : null;
        if (TextUtils.isEmpty(base64EncodedPublicKey)) {
            AppState.setBoolean(AppState.SHOW_DONATE, false);
            return;
        }

        // talk to the store
        BillingHelper.startSetup(this, base64EncodedPublicKey, new BillingHelper.OnSetupFinishedListener() {
            @Override
            public void onSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    boolean showDonateButton = BillingHelper.showDonateButton(MainActivity.this);
                    Log.d(TAG, "onSetupFinished showDonate="+showDonateButton);
                    AppState.setBoolean(AppState.SHOW_DONATE, showDonateButton);
                }
            }
        });
    }

    void initMenuProvider() {
        menuProvider = MenuProvider.create(getString(R.string.ou_menu_provider));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult req="+requestCode+" result="+resultCode);
        if (resultCode == BillingHelper.RESULT_PURCHASED) {
            AppState.setBoolean(AppState.SHOW_DONATE, false); // hide button on any purchase
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // remove items we don't need
        if (TextUtils.isEmpty(getFeedbackUrl()))
            menu.removeItem(R.id.action_feedback);
        boolean showDonate = AppState.getBoolean(AppState.SHOW_DONATE, false);
        if (!showDonate)
            menu.removeItem(R.id.action_donate);
        if (!getResources().getBoolean(R.bool.ou_enable_messages))
            menu.removeItem(R.id.action_messages);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        menu.getItem(0).setEnabled(AccountUtils.haveMoreThanOneAccount(this));
        return true;
    }

    @SuppressLint("StringFormatInvalid")
    protected String getFeedbackUrl() {
        String urlFormat = getString(R.string.ou_feedback_url);
        if (urlFormat.isEmpty())
            return urlFormat;
        return String.format(urlFormat, getAppVersionCode(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (menuProvider != null && menuProvider.onItemSelected(this, id))
            return true;
        if (ActionBarUtils.handleNavigationAction(this, id))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.account_btn_check_out) {
            startActivity(new Intent(this, ItemsCheckOutListView.class));
        } else if (id == R.id.account_btn_holds) {
            startActivity(new Intent(this, HoldsListView.class));
        } else if (id == R.id.account_btn_fines) {
            startActivity(new Intent(this, FinesActivity.class));
        } else if (id == R.id.main_my_lists_button) {
            startActivity(new Intent(this, BookBagListView.class));
        } else if (id == R.id.main_btn_search) {
            startActivity(new Intent(this, SearchActivity.class));
        } else if (menuProvider != null) {
            menuProvider.onItemSelected(this, id);
        }
    }
}
