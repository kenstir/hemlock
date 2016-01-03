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

package org.evergreen_ils.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kenstir on 1/2/2016.
 */
public class BillingHelper {
    public static final String TAG = "Billing";
    public static final String SKU_GOLD = "gold";
    public static final String SKU_SILVER = "silver";
    public static final String SKU_BRONZE = "bronze";
    public static final String SKU_KARMA = "karma";
    // (arbitrary) request code for the purchase flow
    public static final int RC_REQUEST = 10001;

    public interface OnSetupFinishedListener {
        void onSetupFinished(IabResult result);
    }
    public interface OnPurchaseFinishedListener {
        void onPurchaseFinished(IabResult result);
    }

    static long mStartTime;
    static IabHelper mHelper;
    static OnSetupFinishedListener mSetupFinishedListener;
    static Inventory mInventory;

    public static void startSetup(Context context, String base64EncodedPublicKey, final OnSetupFinishedListener listener) {
        mStartTime = System.currentTimeMillis();
        mHelper = new IabHelper(context, base64EncodedPublicKey);
        if (GlobalConfigs.isDebuggable())
            mHelper.enableDebugLogging(true);
        mSetupFinishedListener = listener;
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "[" + getElapsedMillis() + "ms] setup finished with result " + result);
                if (result.isFailure() || mHelper == null) {
                    mSetupFinishedListener.onSetupFinished(result);
                } else {
                    mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            Log.d(TAG, "[" + getElapsedMillis() + "ms] queryInventory finished with result " + result);
                            onInventoryAvailable(inv);
                            mSetupFinishedListener.onSetupFinished(result);
                        }
                    });
                }
            }
        });
    }

    public static void launchPurchaseFlow(final Activity activity, String sku, final OnPurchaseFinishedListener listener) {
        if (mHelper == null) {
            Log.d(TAG, "launchPurchaseFlow: null helper");
            listener.onPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE,
                    "service unavailable"));
            return;
        }

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "random";

        Log.d(TAG, "launchPurchaseFlow");
        BillingHelper.getIabHelper().launchPurchaseFlow(activity, sku, BillingHelper.RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Log.d(TAG, "purchase finished with result " + result);
                if (result.isSuccess()) {
                    mInventory.addPurchase(purchase);
                    consumeKarma();
                }
                listener.onPurchaseFinished(result);
            }
        }, payload);
    }

    public static boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (mHelper == null)
            return false;
        return mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    private static void onInventoryAvailable(Inventory inv) {
        Log.d(TAG, "inv=" + inv);
        if (inv == null || mHelper == null) {
            mInventory = new Inventory();
            return;
        }
        mInventory = inv;
        List<String> skus = inv.getAllOwnedSkus();
        Log.d(TAG, "skus: " + TextUtils.join(", ", skus));

        consumeKarma();
    }

    // If we have any karma, consume it.  This allows for multiple indulgences.
    private static void consumeKarma() {
        Purchase karmaPurchase = mInventory.getPurchase(BillingHelper.SKU_KARMA);
        if (karmaPurchase != null && mHelper != null) {
            Log.d(TAG, "We have karma. Consuming purchase " + karmaPurchase.getOrderId());
            mHelper.consumeAsync(karmaPurchase, new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (result.isSuccess()) {
                        mInventory.erasePurchase(BillingHelper.SKU_KARMA);
                    }
                }
            });
        }
    }

    public static IabHelper getIabHelper() {
        return mHelper;
    }

    public static long getElapsedMillis() {
        return System.currentTimeMillis() - mStartTime;
    }

    public static void disposeIabHelper() {
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    public static boolean showDonateButton() {
        // todo better logic here
        return true;
    }
}
