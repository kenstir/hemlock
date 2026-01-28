/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.ui.view.main

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.MenuItemCompat
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.PatronMessage
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.launchURL
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.BarcodeActivity
import net.kenstir.ui.view.FinesActivity
import net.kenstir.ui.view.OrgDetailsActivity
import net.kenstir.ui.view.bookbags.BookBagsActivity
import net.kenstir.ui.view.checkouts.CheckoutsActivity
import net.kenstir.ui.view.holds.HoldsActivity
import net.kenstir.ui.view.search.SearchActivity

open class MainActivity : MainBaseActivity() {

    private var mUnreadMessageCount: Int? = null //unknown
    private var mUnreadMessageText: TextView? = null
    private var eventsButton: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

//        if (onCreateHandleLaunchIntent()) return

        eventsButton = findViewById(R.id.main_events_button)
        setupEventsButton()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")
        fetchData()

        // Debugging crashes reported to Crashlytics.
        // Every occurrence is Nexus 5X Android 8.1.0
        // Fatal Exception: java.lang.RuntimeException: Unable to start activity ComponentInfo{net.kenstir.apps.indiana/org.evergreen_ils.views.launch.LaunchActivity}: android.view.InflateException: Binary XML file line #43: Binary XML file line #43: Error inflating class ImageView
        // Caused by android.view.InflateException: Binary XML file line #43: Binary XML file line #43: Error inflating class ImageView
        // Caused by android.content.res.Resources$NotFoundException: Drawable (missing name) with resource ID #0x7f0800c4
//        val resourceName = resources.getResourceName(0x7f0800c3) // net.kenstir.apps.pines:drawable/notification_bg_low
//        val resourceName = resources.getResourceName(0x7f0800c4) // net.kenstir.apps.pines:drawable/notify_panel_notification_icon_bg
//        val resourceName = resources.getResourceName(0x7f0800e1) // net.kenstir.apps.pines:drawable/splash_title
//        showAlert("$resourceName")
    }

    private fun fetchData() {
        initializePushNotifications()
        loadUnreadMessageCount()
    }

    private fun setupEventsButton() {
        // hide Events button if not enabled, or if the org has no eventsURL
        if (!homeOrgHasEvents()) {
            eventsButton?.visibility = View.GONE
        }
    }

    private fun homeOrgHasEvents(): Boolean {
        val url = getEventsUrl()
        return resources.getBoolean(R.bool.ou_enable_events_button) && !url.isNullOrEmpty()
    }

    private fun loadUnreadMessageCount() {
        Log.d(TAG, "[async] loadUnreadMessageCount ...")
        scope.async {
            if (resources.getBoolean(R.bool.ou_enable_messages)) {
                val start = System.currentTimeMillis()
                val result = App.svc.user.fetchPatronMessages(
                    App.account)
                Log.logElapsedTime(TAG, start, "[async] fetchUserMessages ... done")
                when (result) {
                    is Result.Success ->  updateMessagesBadge(result.get())
                    is Result.Error -> showAlert(result.exception)
                }
            }
        }
    }

    private fun updateMessagesBadge(messages: List<PatronMessage>) {
        mUnreadMessageCount = messages.count {
            it.isPatronVisible && !it.isDeleted && !it.isRead
        }
        updateUnreadMessagesText()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult req=$requestCode result=$resultCode")
        if (requestCode == App.REQUEST_MESSAGES) {
            loadUnreadMessageCount()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // remove items we don't need
        if (TextUtils.isEmpty(feedbackUrl))
            menu.removeItem(R.id.action_feedback)

        // set up the messages action view, it didn't work when set in xml
        if (resources.getBoolean(R.bool.ou_enable_messages)) {
            createMessagesActionView(menu)
        } else {
            menu.removeItem(R.id.action_messages)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
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
        button.contentDescription = getString(R.string.menu_messages)
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
        } else if (id == R.id.main_events_button) {
            launchURL(getEventsUrl())
        } else if (id == R.id.main_showcard_button) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (menuItemHandler != null) {
            menuItemHandler?.onItemSelected(this, id, "main_button")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
