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

package org.evergreen_ils.system;

import android.content.Context;

import org.evergreen_ils.Api;
import org.evergreen_ils.api.EvergreenService;
import org.opensrf.util.GatewayResult;

import java.util.Map;

public class EvergreenServerLoader {

    private static final String TAG = EvergreenServerLoader.class.getSimpleName();

    private static int mOutstandingRequests = 0;
    private static long start_ms = 0;

    private static Boolean parseBoolSetting(GatewayResult response, String setting) {
        Boolean value = null;
        try {
            Map<String, ?> resp_map = (Map<String, ?>) response.payload;
            Object o = resp_map.get(setting);
            if (o != null) {
                Map<String, ?> resp_setting_map = (Map<String, ?>)o;
                value = Api.parseBoolean(resp_setting_map.get("value"));
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        return value;
    }

    private static void parseOrgSettingsFromGatewayResponse(GatewayResult response, final Organization org) {
        Boolean not_pickup_lib = parseBoolSetting(response, Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
        if (not_pickup_lib != null)
            org.settingIsPickupLocation = !not_pickup_lib;
        Boolean allow_credit_payments = parseBoolSetting(response, Api.SETTING_CREDIT_PAYMENTS_ALLOW);
        if (allow_credit_payments != null)
            org.settingAllowCreditPayments = allow_credit_payments;
        Boolean sms_enable = parseBoolSetting(response, Api.SETTING_SMS_ENABLE);
        if (sms_enable != null)
            EvergreenService.Companion.setSmsEnabled(sms_enable);

        org.settingsLoaded = true;
    }

    // fetch settings that we need for all orgs
    public static void fetchOrgSettings(Context context) {
        /*
        startVolley();
        final EvergreenServer eg = EvergreenServer.getInstance();
        final AccountAccess ac = AccountAccess.getInstance();
        final Organization home_org = EvergreenService.Companion.findOrg(ac.getHomeLibraryID());
        final Organization pickup_org = EvergreenService.Companion.findOrg(ac.getDefaultPickupLibraryID());

        // To minimize risk of race condition, load home and pickup orgs first.
        // Use a clone so we don't screw up the search org spinner.
        // TODO: replace with coroutines
        ArrayList<Organization> orgs = new ArrayList<>();//(ArrayList<Organization>) EvergreenService.Companion.getOrgs().clone();
        if (home_org != null) {
            orgs.remove(home_org);
            orgs.add(0, home_org);
        }
        if (pickup_org != null) {
            orgs.remove(pickup_org);
            orgs.add(0, pickup_org);
        }

        for (final Organization org : orgs) {
            if (org.settingsLoaded)
                continue;
            ArrayList<String> settings = new ArrayList<>();
            settings.add(Api.SETTING_ORG_UNIT_NOT_PICKUP_LIB);
            settings.add(Api.SETTING_CREDIT_PAYMENTS_ALLOW);
            if (org.parent_ou == null) {
                settings.add((Api.SETTING_SMS_ENABLE));
            }
            final String method = Api.ORG_UNIT_SETTING_BATCH;
            String url = eg.getUrl(Analytics.buildGatewayUrl(
                    Api.ACTOR, method,
                    new Object[]{org.id, settings, ac.getAuthToken()}));
            ...
        }
        */
    }
}
