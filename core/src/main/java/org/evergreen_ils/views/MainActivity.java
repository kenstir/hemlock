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
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.accountAccess.bookbags.BookBagActivity;
import org.evergreen_ils.accountAccess.checkout.CheckoutsActivity;
import org.evergreen_ils.accountAccess.fines.FinesActivity;
import org.evergreen_ils.accountAccess.holds.HoldsActivity;
import org.evergreen_ils.android.App;
import org.evergreen_ils.billing.BillingDataProvider;
import org.evergreen_ils.billing.BillingHelper;
import org.evergreen_ils.billing.IabResult;
import org.evergreen_ils.searchCatalog.SearchActivity;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.system.EvergreenServerLoader;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.AppState;
import org.evergreen_ils.utils.ui.BaseActivity;

/**
 * Created by kenstir on 12/28/13.
 */
public class MainActivity extends BaseActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    private Integer mUnreadMessageCount = null; //unknown
    private TextView mUnreadMessageText = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mRestarting) return;

        setContentView(R.layout.activity_main);

        EvergreenServerLoader.fetchOrgSettings(this);
        EvergreenServerLoader.fetchSMSCarriers(this);
        fetchUnreadMessageCount();
        initBillingProvider();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult req="+requestCode+" result="+resultCode);
        if (requestCode == App.REQUEST_PURCHASE && resultCode == App.RESULT_PURCHASED) {
            AppState.setBoolean(AppState.SHOW_DONATE, false); // hide button on any purchase
        } else if (requestCode == App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT) {
            fetchUnreadMessageCount();
        }
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

        // set up the messages action view, it didn't work when set in xml
        if (!getResources().getBoolean(R.bool.ou_enable_messages)) {
            menu.removeItem(R.id.action_messages);
        } else {
            createMessagesActionView(menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem item = menu.findItem(R.id.action_switch_account);
        if (item != null)
            item.setEnabled(AccountUtils.haveMoreThanOneAccount(this));
        updateUnreadMessageText();
        return true;
    }

    private void createMessagesActionView(Menu menu) {
        final MenuItem item = menu.findItem(R.id.action_messages);
        MenuItemCompat.setActionView(item, R.layout.badge_layout);
        RelativeLayout layout = (RelativeLayout) MenuItemCompat.getActionView(item);
        mUnreadMessageText = (TextView) layout.findViewById(R.id.badge_text);
        ImageButton button = (ImageButton) layout.findViewById(R.id.badge_icon_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(item);
            }
        });
    }

    private void updateUnreadMessageText() {
        if (mUnreadMessageText == null)
            return;
        if (mUnreadMessageCount != null) {
            mUnreadMessageText.setVisibility((mUnreadMessageCount > 0) ? View.VISIBLE : View.GONE);
            mUnreadMessageText.setText(String.format("%d", mUnreadMessageCount));
        } else {
            mUnreadMessageText.setVisibility(View.GONE);
        }
    }
    
    private void fetchUnreadMessageCount() {
        if (!getResources().getBoolean(R.bool.ou_enable_messages))
            return;
        EvergreenServerLoader.fetchUnreadMessageCount(this, new EvergreenServerLoader.OnResponseListener<Integer>() {
            @Override
            public void onResponse(Integer count) {
                mUnreadMessageCount = count;
                updateUnreadMessageText();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mMenuItemHandler != null && mMenuItemHandler.onItemSelected(this, id, "main_option_menu"))
            return true;
        if (handleMenuAction(id))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.account_btn_check_out) {
            Analytics.logEvent("Checkouts: Open", "via", "main_button");
            startActivity(new Intent(this, CheckoutsActivity.class));
        } else if (id == R.id.account_btn_holds) {
            Analytics.logEvent("Holds: Open", "via", "main_button");
            startActivity(new Intent(this, HoldsActivity.class));
        } else if (id == R.id.account_btn_fines) {
            Analytics.logEvent("Fines: Open", "via", "main_button");
            startActivity(new Intent(this, FinesActivity.class));
        } else if (id == R.id.main_my_lists_button) {
            Analytics.logEvent("Lists: Open", "via", "main_button");
            startActivity(new Intent(this, BookBagActivity.class));
        } else if (id == R.id.main_btn_search) {
            Analytics.logEvent("Search: Open", "via", "main_button");
            startActivity(new Intent(this, SearchActivity.class));
        } else if (id == R.id.main_barcode_button) {
            Analytics.logEvent("Barcode: Open", "via", "main_button");
            startActivity(new Intent(this, BarcodeActivity.class));
        } else if (mMenuItemHandler != null) {
            mMenuItemHandler.onItemSelected(this, id, "main_button");
        }
    }
}
