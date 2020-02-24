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

package net.kenstir.apps.pines;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.evergreen_ils.android.Analytics;
import org.evergreen_ils.views.MenuProvider;

/**
 * Created by kenstir on 1/28/2017.
 */
public class PinesMenuProvider extends MenuProvider {

    @Override
    public void onCreate(Activity activity) {
        return;
        /*
        AccountAccess ac = AccountAccess.getInstance();
        Integer unread = ac.getUnreadMessageCount();
        if (unread > 0) {
            Button b = (Button)activity.findViewById(R.id.patron_message_center);
            b.setText("" + unread + " messages");
        }
        */
    }

    @Override
    public boolean onItemSelected(Activity activity, int id, String via) {
        if (id == R.id.open_full_catalog_button) {
            Analytics.logEvent("Link: Full Catalog", "via", via);
            String url = activity.getString(org.evergreen_ils.R.string.ou_library_url);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else if (id == R.id.library_locator_button) {
            Analytics.logEvent("Link: Library Locator", "via", via);
            String url = "http://pines.georgialibraries.org/pinesLocator/locator.html";
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else {
            return false;
        }
        return true;
    }
}
