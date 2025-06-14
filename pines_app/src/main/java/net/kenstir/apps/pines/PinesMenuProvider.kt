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
package net.kenstir.apps.pines

import android.app.Activity
import android.content.Intent
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat.startActivityForResult
import org.evergreen_ils.android.App.REQUEST_MESSAGES
import org.evergreen_ils.net.Gateway.getUrl
import org.evergreen_ils.utils.ui.ActivityUtils
import org.evergreen_ils.views.MenuProvider
import org.evergreen_ils.views.messages.MessagesActivity

@Keep
class PinesMenuProvider : MenuProvider() {
    override fun onCreate(activity: Activity) {
        return
    }

    override fun onItemSelected(activity: Activity, id: Int, via: String): Boolean {
        if (id == R.id.open_full_catalog_button) {
            //Analytics.logEvent("fullcatalog_click", "via", via);
            val url = activity.getString(R.string.ou_library_url)
            ActivityUtils.launchURL(activity, url)
        } else if (id == R.id.library_locator_button) {
            //Analytics.logEvent("librarylocator_click", "via", via);
            val url = "https://pines.georgialibraries.org/pinesLocator/locator.html"
            ActivityUtils.launchURL(activity, url)
        } else if (id == R.id.galileo_button) {
            //Analytics.logEvent("galileo_click", "via", via);
            val url = "https://www.galileo.usg.edu"
            ActivityUtils.launchURL(activity, url)
        } else if (id == R.id.patron_message_center) {
            //Analytics.logEvent("messages_click", "via", "options_menu");
            activity.startActivityForResult(Intent(activity, MessagesActivity::class.java), REQUEST_MESSAGES)
        } else {
            return false
        }
        return true
    }
}
