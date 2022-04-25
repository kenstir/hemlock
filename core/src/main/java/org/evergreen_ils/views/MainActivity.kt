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

package org.evergreen_ils.views

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.MenuItemCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.evergreen_ils.R
import org.evergreen_ils.android.AccountUtils
import org.evergreen_ils.views.bookbags.BookBagsActivity
import org.evergreen_ils.views.holds.HoldsActivity
import org.evergreen_ils.android.App
import org.evergreen_ils.system.EgSms
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.searchCatalog.SearchActivity
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.Log
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.showAlert
import org.opensrf.util.OSRFObject

class MainActivity : BaseActivity() {

    private var mUnreadMessageCount: Int? = null //unknown
    private var mUnreadMessageText: TextView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
        fetchData()

        // Debugging crashes reported to Crashlytics.
        // Every occurrence is Nexus 5X Android 8.1.0
        // Fatal Exception: java.lang.RuntimeException: Unable to start activity ComponentInfo{net.kenstir.apps.indiana/org.evergreen_ils.views.launch.LaunchActivity}: android.view.InflateException: Binary XML file line #43: Binary XML file line #43: Error inflating class ImageView
        // Caused by android.view.InflateException: Binary XML file line #43: Binary XML file line #43: Error inflating class ImageView
        // Caused by android.content.res.Resources$NotFoundException: Drawable (missing name) with resource ID #0x7f0800c4
//        val resourceName = resources.getResourceName(0x7f0800c3) // net.kenstir.apps.pines:drawable/notification_bg_low
//        val resourceName = resources.getResourceName(0x7f0800c4) // net.kenstir.apps.pines:drawable/notify_panel_notification_icon_bg
//        showAlert("$resourceName")
    }

    private fun fetchData() {
        loadSMSCarriers()
        loadUnreadMessageCount()
    }

    // TODO: Make this on demand by making it a suspend fun in GatewayLoader.
    private fun loadSMSCarriers() {
        Log.d(TAG, "[async] fetchSMSCarriers ...")
        async {
            val start = System.currentTimeMillis()
            val result = Gateway.pcrud.fetchSMSCarriers()
            Log.logElapsedTime(TAG, start, "[async] fetchSMSCarriers ... done")
            when (result) {
                is Result.Success -> EgSms.loadCarriers(result.data)
                is Result.Error -> showAlert(result.exception)
            }
        }
    }

    private fun loadUnreadMessageCount() {
        Log.d(TAG, "[async] fetchUserMessages ...")
        async {
            if (resources.getBoolean(R.bool.ou_enable_messages)) {
                val start = System.currentTimeMillis()
                val result = Gateway.actor.fetchUserMessages(App.getAccount())
                Log.logElapsedTime(TAG, start, "[async] fetchUserMessages ... done")
                when (result) {
                    is Result.Success ->  updateMessagesBadge(result.data)
                    is Result.Error -> showAlert(result.exception)
                }
            }
        }
    }

    private fun updateMessagesBadge(messages: List<OSRFObject>) {
        mUnreadMessageCount = countUnread(messages)
        updateUnreadMessagesText()
    }

    private fun countUnread(messages: List<OSRFObject>): Int {
        var count = 0
        messages.forEach {
            val readDate = it.getString("read_date")
            val deleted = it.getBoolean("deleted")
            val patronVisible = it.getBoolean("pub")
            if (readDate == null && !deleted && patronVisible) {
                ++count
            }
        }
        return count
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult req=$requestCode result=$resultCode")
        if (requestCode == App.REQUEST_MYOPAC_MESSAGES) {
            loadUnreadMessageCount()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)

        // remove items we don't need
        if (TextUtils.isEmpty(feedbackUrl))
            menu.removeItem(R.id.action_feedback)

        // set up the messages action view, it didn't work when set in xml
        if (!resources.getBoolean(R.bool.ou_enable_messages)) {
            menu.removeItem(R.id.action_messages)
        } else {
            createMessagesActionView(menu)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_switch_account)
        if (item != null)
            item.isEnabled = AccountUtils.haveMoreThanOneAccount(this)
        updateUnreadMessagesText()
        return true
    }

    private fun createMessagesActionView(menu: Menu) {
        val item = menu.findItem(R.id.action_messages)
        MenuItemCompat.setActionView(item, R.layout.badge_layout)
        val layout = MenuItemCompat.getActionView(item) as RelativeLayout
        mUnreadMessageText = layout.findViewById<View>(R.id.badge_text) as TextView
        val button = layout.findViewById<View>(R.id.badge_icon_button) as ImageButton
        button.setOnClickListener { onOptionsItemSelected(item) }
    }

    private fun updateUnreadMessagesText() {
        if (mUnreadMessageText == null)
            return
        if (isFinishing)
            return
        val count = mUnreadMessageCount
        if (count != null) {
            mUnreadMessageText?.visibility = if (count > 0) View.VISIBLE else View.GONE
            mUnreadMessageText?.text = String.format("%d", count)
        } else {
            mUnreadMessageText?.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (menuItemHandler?.onItemSelected(this, id, "main_option_menu") == true)
            return true
        return if (handleMenuAction(id)) true else super.onOptionsItemSelected(item)
    }

    fun onButtonClick(v: View) {
        //throw RuntimeException("Test Crash") // test Crashlytics
        val id = v.id
        if (id == R.id.main_checkouts_button) {
            startActivity(Intent(this, CheckoutsActivity::class.java))
        } else if (id == R.id.main_holds_button) {
            startActivity(Intent(this, HoldsActivity::class.java))
        } else if (id == R.id.main_fines_button) {
            startActivity(Intent(this, FinesActivity::class.java))
        } else if (id == R.id.main_my_lists_button) {
            startActivity(Intent(this, BookBagsActivity::class.java))
        } else if (id == R.id.main_search_button) {
            startActivity(Intent(this, SearchActivity::class.java))
        } else if (id == R.id.main_library_info_button) {
            startActivity(Intent(this, OrgDetailsActivity::class.java))
        } else if (id == R.id.main_showcard_button) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (menuItemHandler != null) {
            menuItemHandler?.onItemSelected(this, id, "main_button")
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
