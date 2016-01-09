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
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Toast;
import org.evergreen_ils.R;
import org.evergreen_ils.billing.BillingHelper;
import org.evergreen_ils.billing.IabResult;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.util.HashMap;

/**
 * Created by kenstir on 1/1/2016.
 */
public class DonateActivity extends ActionBarActivity {
    private final static String TAG = DonateActivity.class.getSimpleName();
    private ProgressDialog progressDialog;
    private SoundPool soundPool;
    private HashMap<String,Integer> soundPoolMap;
    private HashMap<String,String> attributionMap;

    private void initSounds(Context context) {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        Log.d(TAG, "initSounds soundPool="+soundPool);
        if (soundPool == null)
            return;
        soundPoolMap = new HashMap<String, Integer>(3);
        soundPoolMap.put(BillingHelper.SKU_KARMA, soundPool.load(context, R.raw.metal_gong, 1));
        soundPoolMap.put(BillingHelper.SKU_SILVER, soundPool.load(context, R.raw.small_crowd_applause, 1));
        soundPoolMap.put(BillingHelper.SKU_GOLD, soundPool.load(context, R.raw.ten_second_applause, 1));
        attributionMap = new HashMap<String, String>(3);
        attributionMap.put(BillingHelper.SKU_KARMA, "Metal Gong 1 by Dianakc, soundbible.com, CC BY 3.0");
        attributionMap.put(BillingHelper.SKU_SILVER, "Small Crowd Applause by Yannick Lemieux, soundbible.com, CC BY 3.0");
        attributionMap.put(BillingHelper.SKU_GOLD, "10 Second Applause by Mike Koenig, soundbible.com, CC BY 3.0");
        Log.d(TAG, "initSounds done)");
    }

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
    protected void onResume() {
        initSounds(this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
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
                if (result.isSuccess()) {
                    showThanks(sku);
                    setResult(BillingHelper.RESULT_PURCHASED);
                    //finish();
                } else {
                    Toast.makeText(DonateActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        Log.d(TAG, "launchPurchaseFlow exiting");
    }

    public void showThanks(final String sku) {
        Log.d(TAG, "showThanks sku=" + sku + " soundPool=" + soundPool);
        String attribution = "";
        if (soundPool != null) {
            float volume = 1f;
            int ret = soundPool.play(soundPoolMap.get(sku), volume, volume, 1, 0, 1f);
            Log.d(TAG, "showThanks play returned "+ret);
            attribution += "\n" + attributionMap.get(sku);
        }
        Log.d(TAG, "showThanks attrib="+attribution);
        Toast.makeText(this, "Thanks!" + attribution, Toast.LENGTH_LONG).show();
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
