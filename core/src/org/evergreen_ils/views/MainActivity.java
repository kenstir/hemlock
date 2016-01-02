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
import org.evergreen_ils.billing.*;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.SearchCatalogListView;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.util.List;

/**
 * Created by kenstir on 12/28/13.
 */
public class MainActivity extends ActionBarActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    IabHelper mHelper;
    private static String SKU_BRONZE = "bronze";
    private static String SKU_SILVER = "silver";
    private static String SKU_GOLD = "gold";
    private static String SKU_KARMA = "karma";

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

        initBilling();
    }

    private void initBilling() {
        // todo obfuscate the public key
        BillingDataProvider provider = BillingDataProvider.create(getString(R.string.ou_billing_data_provider));
        String base64EncodedPublicKey = (provider != null) ? provider.getPublicKey() : null;
        if (TextUtils.isEmpty(base64EncodedPublicKey)) {
            findViewById(R.id.main_donate_button).setVisibility(View.GONE);
            return;
        }

        mHelper = new IabHelper(this, base64EncodedPublicKey);
        if (GlobalConfigs.isDebuggable())
            mHelper.enableDebugLogging(true);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "setup finished, result="+result);
                if (result.isFailure()) {
                    // report this
                    return;
                }
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null)
                    return;
                Log.d(TAG, "querying inventory");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished, result="+result);

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                // todo report this
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            List<String> skus = inventory.getAllOwnedSkus();
            Log.d(TAG, "skus="+TextUtils.join(",", skus));

            // if we own karma, consume it
            Purchase karmaPurchase = inventory.getPurchase(SKU_KARMA);
            if (karmaPurchase != null) {
                Log.d(TAG, "We have karma. Consuming it.");
                mHelper.consumeAsync(inventory.getPurchase(SKU_KARMA), null);
                return;
            }
        }
    };

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
