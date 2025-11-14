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
import androidx.core.os.bundleOf
import net.kenstir.ui.App.REQUEST_MESSAGES
import net.kenstir.ui.util.ActivityUtils
import net.kenstir.ui.view.MenuProvider
import net.kenstir.ui.view.messages.MessagesActivity
import net.kenstir.util.Analytics

@Keep
@Suppress("unused")
class PinesMenuProvider : MenuProvider() {
    override fun onCreate(activity: Activity) {
        return
    }

    override fun onItemSelected(activity: Activity, id: Int, via: String): Boolean {
        when (id) {
            R.id.open_full_catalog_button -> {
                Analytics.logEvent(Analytics.Event.OTHER_ACTION, bundleOf(
                    Analytics.Param.ACTION_NAME to "full_catalog",
                ))
                val url = activity.getString(R.string.ou_library_url)
                ActivityUtils.launchURL(activity, url)
            }
            R.id.library_locator_button -> {
                Analytics.logEvent(Analytics.Event.OTHER_ACTION, bundleOf(
                    Analytics.Param.ACTION_NAME to "library_locator",
                ))
                val url = "https://pines.georgialibraries.org/pinesLocator/locator.html"
                ActivityUtils.launchURL(activity, url)
            }
            R.id.galileo_button -> {
                Analytics.logEvent(Analytics.Event.OTHER_ACTION, bundleOf(
                    Analytics.Param.ACTION_NAME to "galileo",
                ))
                val url = "https://www.galileo.usg.edu"
                ActivityUtils.launchURL(activity, url)
            }
            R.id.patron_message_center -> {
                Analytics.logEvent(Analytics.Event.OTHER_ACTION, bundleOf(
                    Analytics.Param.ACTION_NAME to "messages",
                ))
                activity.startActivityForResult(Intent(activity, MessagesActivity::class.java), REQUEST_MESSAGES)
            }
            else -> {
                return false
            }
        }
        return true
    }
}
