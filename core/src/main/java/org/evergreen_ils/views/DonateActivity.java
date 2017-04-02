/*
 * Copyright (C) 2016 Kenneth H. Cox
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import org.evergreen_ils.R;
import org.evergreen_ils.android.App;
import org.evergreen_ils.billing.BillingHelper;
import org.evergreen_ils.billing.IabResult;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.util.HashMap;

/**
 * Created by kenstir on 1/1/2016.
 */
public class DonateActivity extends AppCompatActivity {
    private final static String TAG = DonateActivity.class.getSimpleName();
    private ProgressDialog progressDialog;
    private HashMap<String,Integer> mediaResourceIdMap;
    private HashMap<String,String> attributionMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.activity_donate);
        ActionBarUtils.initActionBarForActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initSounds(this);
    }

    private void initSounds(Context context) {
        Log.d(TAG, "initSounds start");
        mediaResourceIdMap = new HashMap<String, Integer>(3);
        mediaResourceIdMap.put(BillingHelper.SKU_KARMA, R.raw.metal_gong);
        mediaResourceIdMap.put(BillingHelper.SKU_SILVER, R.raw.small_crowd_applause);
        mediaResourceIdMap.put(BillingHelper.SKU_GOLD, R.raw.ten_second_applause);
        attributionMap = new HashMap<String, String>(3);
        attributionMap.put(BillingHelper.SKU_KARMA, "Metal Gong 1 by Dianakc, soundbible.com, CC BY 3.0");
        attributionMap.put(BillingHelper.SKU_SILVER, "Small Crowd Applause by Yannick Lemieux, soundbible.com, CC BY 3.0");
        attributionMap.put(BillingHelper.SKU_GOLD, "10 Second Applause by Mike Koenig, soundbible.com, CC BY 3.0");
        Log.d(TAG, "initSounds done");
    }

    void setBusy(boolean set) {
        Log.d(TAG, "setBusy "+set);
        if (set) {
            progressDialog = ProgressDialog.show(this, "", "One sec...");
        } else if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    void launchPurchaseFlow(final String sku) {
        setBusy(true);
        Log.d(TAG, "Launching purchase flow for " + sku);

        BillingHelper.launchPurchaseFlow(this, sku, new BillingHelper.OnPurchaseFinishedListener() {
            @Override
            public void onPurchaseFinished(IabResult result) {
                Log.d(TAG, "onPurchaseFinished result="+result);
                setBusy(false);

                // debug apps cannot make purchases, just pretend
                boolean isSuccess = App.getIsDebuggable(DonateActivity.this) ? true : result.isSuccess();
                if (isSuccess) {
                    giveThanks(sku);
                } else {
                    Toast.makeText(DonateActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        Log.d(TAG, "launchPurchaseFlow exiting");
    }

    public void postPurchaseFinishActivity() {
        setResult(BillingHelper.RESULT_PURCHASED);
        finish();
    }

    public void giveThanks(final String sku) {
        Log.d(TAG, "giveThanks sku=" + sku);
        String attribution = "";
        Integer res = mediaResourceIdMap.get(sku);
        if (res != null) {
            MediaPlayer mp = MediaPlayer.create(this, res);
            // looks like I don't need this...the media will continue playing even in the parent activity
//            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    postPurchaseFinishActivity();
//                }
//            });
            mp.start();
        }
        postPurchaseFinishActivity();
        Toast.makeText(this, getString(R.string.toast_thanks) + attribution, Toast.LENGTH_LONG).show();
    }

    // called when purchase flow finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (!BillingHelper.handleActivityResult(requestCode, resultCode, data)) {
            Log.d(TAG, "onActivityResult not handled, going super");
            super.onActivityResult(requestCode, resultCode, data);
        }
        Log.d(TAG, "onActivityResult out of block");

        // unnecessary but being safe here
        setBusy(false);
    }

    public void onButtonClick(View v) {
        int id = v.getId();
        if (id == R.id.donate_karma_button) {
            launchPurchaseFlow(BillingHelper.SKU_KARMA);
        } else if (id == R.id.donate_silver_button) {
            launchPurchaseFlow(BillingHelper.SKU_SILVER);
        } else if (id == R.id.donate_gold_button) {
            launchPurchaseFlow(BillingHelper.SKU_GOLD);
        }
    }
}
