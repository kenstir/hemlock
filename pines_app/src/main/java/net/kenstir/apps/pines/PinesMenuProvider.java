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
import android.widget.Button;
import org.evergreen_ils.views.MenuProvider;

/**
 * Created by kenstir on 1/28/2017.
 */
public class PinesMenuProvider extends MenuProvider {

    @Override
    public void onCreate(Activity activity) {
    }

    @Override
    public void onItemSelected(Activity activity, int id) {
        if (id == R.id.open_full_catalog_button) {
            String url = activity.getString(org.evergreen_ils.R.string.ou_library_url);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else if (id == R.id.library_locator_button) {
            String url = "http://pines.georgialibraries.org/pinesLocator/locator.html";
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }
}