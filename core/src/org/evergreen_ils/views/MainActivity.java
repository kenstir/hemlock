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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsListView;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.billing.*;
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
    private boolean showDonateButton;
    private boolean donateButtonShowing;
    private Button donateButton;
    private int mAnimationDuration;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.activity_main);
        ActionBarUtils.initActionBarForActivity(this, true);

        // singleton initialize necessary IDL and Org data
//        globalConfigs = GlobalConfigs.getInstance(this);

        // hide the donate button until we know what the deal is
        donateButton = (Button) findViewById(R.id.main_donate_button);
        donateButton.setVisibility(View.INVISIBLE);
        donateButtonShowing = false;
        //mAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mAnimationDuration = 3000;

        initBilling();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        BillingHelper.disposeIabHelper();
    }

    void initBilling() {
        //todo remove these views
        int app_launches = BillingHelper.getAppLaunches();
        float days_installed = BillingHelper.getDaysInstalled();
        ((TextView)findViewById(R.id.textView)).setText("app launches: " + app_launches);
        ((TextView)findViewById(R.id.textView2)).setText("days installed: " + days_installed);

        // get the public key
        BillingDataProvider provider = BillingDataProvider.create(getString(R.string.ou_billing_data_provider));
        String base64EncodedPublicKey = (provider != null) ? provider.getPublicKey() : null;
        if (TextUtils.isEmpty(base64EncodedPublicKey)) {
            showDonateButton = false;
        } else {
            showDonateButton = BillingHelper.showDonateButton();
        }
        Log.d(TAG, "kcxxx: initBilling showDonate="+showDonateButton);
        updateUi();

        BillingHelper.startSetup(this, base64EncodedPublicKey, new BillingHelper.OnSetupFinishedListener() {
            @Override
            public void onSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    showDonateButton = BillingHelper.showDonateButton();
                    Log.d(TAG, "kcxxx: onSetupFinished showDonate="+showDonateButton);
                    updateUi();
                }
            }
        });
    }

    void updateUi() {
        findViewById(R.id.main_stats_layout).setVisibility(GlobalConfigs.isDebuggable() ? View.VISIBLE : View.GONE);

        float oldOpacity = donateButtonShowing ? 1f : 0f;
        float newOpacity = showDonateButton ? 1f : 0f;
        Log.d(TAG, "kcxxx: updateUi old="+oldOpacity+" new="+newOpacity);
        donateButtonShowing = showDonateButton;
        if (oldOpacity == newOpacity) {
            return;
        }

        /*
        AlphaAnimation anim = new AlphaAnimation(oldOpacity, newOpacity);
        anim.setDuration(mAnimationDuration);
        donateButton.startAnimation(anim);
        */

        donateButton.setAlpha(oldOpacity);
        donateButton.setVisibility(View.VISIBLE);
        donateButton.animate()
                .alpha(newOpacity)
                .setDuration(mAnimationDuration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == BillingHelper.RESULT_PURCHASED) {
            showDonateButton = BillingHelper.showDonateButton();
            updateUi();
        }
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
            startActivity(new Intent(this, ItemsCheckOutListView.class));
        } else if (id == R.id.account_btn_holds) {
            startActivity(new Intent(this, HoldsListView.class));
        } else if (id == R.id.account_btn_fines) {
            startActivity(new Intent(this, FinesActivity.class));
            /*
        } else if (id == R.id.account_btn_book_bags) {
            startActivity(new Intent(this, BookbagsListView.class));
            */
        } else if (id == R.id.main_btn_search) {
            startActivity(new Intent(this, SearchCatalogListView.class));
        } else if (id == R.id.main_donate_button) {
            startActivity(new Intent(this, DonateActivity.class));
        }
    }
}
