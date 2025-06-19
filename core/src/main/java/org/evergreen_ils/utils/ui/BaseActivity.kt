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

package org.evergreen_ils.utils.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.AccountUtils
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.App.REQUEST_MESSAGES
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.android.Log.TAG_FCM
import net.kenstir.hemlock.android.Log.TAG_PERM
import org.evergreen_ils.data.NotificationType
import org.evergreen_ils.data.PushNotification
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.views.*
import org.evergreen_ils.views.bookbags.BookBagsActivity
import org.evergreen_ils.views.holds.HoldsActivity
import org.evergreen_ils.views.messages.MessagesActivity
import org.evergreen_ils.views.search.SearchActivity
import java.net.URLEncoder


/* Activity base class to handle common behaviours like the navigation drawer */
open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var _toolbar: Toolbar? = null
    protected var menuItemHandler: MenuProvider? = null
    protected var isRestarting = false
    var scope = lifecycleScope

    protected val toolbar: Toolbar?
        get() {
            if (_toolbar == null) {
                _toolbar = ActionBarUtils.initActionBarForActivity(this, null, true)
            }
            return _toolbar
        }

    protected val feedbackUrl: String
        @SuppressLint("StringFormatInvalid")
        get() {
            val urlFormat = getString(R.string.ou_feedback_url)
            return if (urlFormat.isEmpty()) urlFormat else String.format(urlFormat, getAppVersionCode(this))
        }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG_PERM, "Permission granted (1)")
            resetDenyCount()
//        } else {
//            //TODO: change to an alert
//            //TODO: change title to "Notice" and add text on how to update it later
//            val msg = getString(R.string.notification_permission_denied_msg, getString(R.string.ou_app_label))
//            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // This logic is a little different from that recommended in the docs at
    // https://developer.android.com/training/permissions/requesting
    // because ActivityCompat.shouldShowRequestPermissionRationale() kept returning true even after
    // the user said "no thanks".  We avoid that case by keeping a deny counter.
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG_PERM, "API Version < 33 did not have a permission dialog")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG_PERM, "Permission granted (2)")
            resetDenyCount()
            return
        }

        // See if the system thinks we should show the rationale dialog but show it only once.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            val denyCount = getDenyCount()
            Log.d(TAG_PERM, "shouldShowRequestPermissionRationale true, denyCount = $denyCount")
            if (denyCount > 0) {
                return
            }
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.notification_permission_rationale_title, getString(R.string.ou_app_label)))
                .setMessage(getString(R.string.notification_permission_rationale_msg))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    Log.d(TAG_PERM, "Launching request for permission (2)")
                    resetDenyCount()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(getString(R.string.no_thanks)) { _, _ ->
                    Log.d(TAG_PERM, "Permission denied")
                    incrementDenyCount()
                }
                .show()
            return
        }

        // Directly ask for the permission
        Log.d(TAG_PERM, "Launching request for permission (1)")
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun resetDenyCount() {
        Log.d(TAG_PERM, "Resetting deny count")
        AppState.setInt(AppState.NOTIFICATIONS_DENY_COUNT, 0)
    }

    private fun incrementDenyCount() {
        val count = AppState.getInt(AppState.NOTIFICATIONS_DENY_COUNT) + 1
        Log.d(TAG_PERM, "Incrementing deny count to $count")
        AppState.setInt(AppState.NOTIFICATIONS_DENY_COUNT, count)
    }

    private fun getDenyCount(): Int {
        val count = AppState.getInt(AppState.NOTIFICATIONS_DENY_COUNT)
        return count
    }

    suspend fun fetchFcmNotificationToken(): Result<Unit> {
        val task = FirebaseMessaging.getInstance().token
        task.await()
        if (!task.isSuccessful) {
            return Result.Error(task.exception ?: Exception("Failed fetching notification token"))
        }
        val token = task.result
        Log.d(TAG_FCM, "fetched fcm token: $token")
        App.setFcmNotificationToken(token)
        return Result.Success(Unit)
    }

    /** Create channels to show notifications.
     * See also: https://developer.android.com/develop/ui/views/notifications/channels
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // NB: The order these are created is the order they appear in Settings>>Notifications
            if (resources.getBoolean(R.bool.notification_channel_register_holds_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.HOLDS.channelId,
                    getString(R.string.notification_channel_holds_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_checkouts_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.CHECKOUTS.channelId,
                    getString(R.string.notification_channel_checkouts_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_fines_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.FINES.channelId,
                    getString(R.string.notification_channel_fines_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            if (resources.getBoolean(R.bool.notification_channel_register_pmc_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.PMC.channelId,
                    getString(R.string.notification_channel_pmc_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            // register the spillover channel last
            if (resources.getBoolean(R.bool.notification_channel_register_general_channel))
                notificationManager.createNotificationChannel(NotificationChannel(
                    NotificationType.GENERAL.channelId,
                    getString(R.string.notification_channel_general_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!App.isStarted()) {
            App.restartApp(this)
            isRestarting = true
            return
        }
        isRestarting = false

        Analytics.initialize(this)
        App.init(this)
        EgSearch.searchLimit = resources.getInteger(R.integer.ou_search_limit)

        initMenuProvider()
        menuItemHandler?.onCreate(this)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        toolbar // has side effect of creating toolbar

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null) {
            val toggle = ActionBarDrawerToggle(
                    this, drawer, _toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            toggle.syncState()
        }

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this)
            val navHeader = navigationView.getHeaderView(0)
            navHeader?.setOnClickListener { v -> onNavigationAction(v.id) }
            if (!resources.getBoolean(R.bool.ou_enable_events_button)) {
                navigationView.menu.removeItem(R.id.main_events_button)
            }
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun initMenuProvider() {
        menuItemHandler = MenuProvider.create(getString(R.string.ou_menu_provider))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_feedback -> {
                Analytics.logEvent(Analytics.Event.FEEDBACK_OPEN)
                launchURL(feedbackUrl)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun getEbooksUrl(): String? {
        return EgOrg.findOrg(App.getAccount().homeOrg)?.eresourcesUrl
    }

    fun getEventsUrl(): String? {
        return EgOrg.findOrg(App.getAccount().homeOrg)?.eventsURL
    }

    fun getMeetingRoomsUrl(): String? {
        return EgOrg.findOrg(App.getAccount().homeOrg)?.meetingRoomsUrl
    }

    fun getMuseumPassesUrl(): String? {
        return EgOrg.findOrg(App.getAccount().homeOrg)?.museumPassesUrl
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return onNavigationAction(id)
    }

    protected fun onNavigationAction(id: Int): Boolean {
        var ret = true
        if (id == R.id.nav_header) {
            startActivity(App.getMainActivityIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } else if (id == R.id.main_search_button) {
            startActivity(Intent(this, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } else if (id == R.id.main_checkouts_button) {
            startActivity(Intent(this, CheckoutsActivity::class.java))
        } else if (id == R.id.main_holds_button) {
            startActivity(Intent(this, HoldsActivity::class.java))
        } else if (id == R.id.main_fines_button) {
            startActivity(Intent(this, FinesActivity::class.java))
        } else if (id == R.id.main_my_lists_button) {
            startActivity(Intent(this, BookBagsActivity::class.java))
        } else if (id == R.id.main_library_info_button) {
            startActivity(Intent(this, OrgDetailsActivity::class.java))
        } else if (id == R.id.main_events_button) {
            launchURL(getEventsUrl())
        } else if (id == R.id.main_showcard_button) {
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (menuItemHandler != null) {
            ret = menuItemHandler!!.onItemSelected(this, id, "nav_drawer")
        } else {
            ret = false
        }

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return ret
    }

    fun handleMenuAction(id: Int): Boolean {
        if (id == R.id.action_switch_account) {
            Analytics.logEvent(Analytics.Event.ACCOUNT_SWITCH)
            App.restartApp(this)
            return true
        } else if (id == R.id.action_add_account) {
            Analytics.logEvent(Analytics.Event.ACCOUNT_ADD)
            invalidateOptionsMenu()
            AccountUtils.addAccount(this) { App.restartApp(this@BaseActivity) }
            return true
        } else if (id == R.id.action_logout) {
            Analytics.logEvent(Analytics.Event.ACCOUNT_LOGOUT)
            logout()
            App.restartApp(this)
            return true
        } else if (id == R.id.action_messages) {
            Analytics.logEvent(Analytics.Event.MESSAGES_OPEN)
            startActivityForResult(Intent(this, MessagesActivity::class.java), REQUEST_MESSAGES)
            return true
        } else if (id == R.id.action_dark_mode) {
            ThemeManager.saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            // return false or else the menu view will be leaked when the activity is restarted
            return false
        } else if (id == R.id.action_light_mode) {
            ThemeManager.saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            // return false or else the menu view will be leaked when the activity is restarted
            return false
        }
        return false
    }

    // Starting with Android 11 (API level 30), you should just catch ActivityNotFoundException;
    // calling resolveActivity requires permission.
    // https://developer.android.com/training/package-visibility/use-cases
    fun launchURL(url: String?, requestId: Int? = null) {
        if (url.isNullOrEmpty()) {
            Toast.makeText(this, R.string.msg_null_url, Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            if (requestId != null) {
                startActivityForResult(intent, requestId)
            } else {
                startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.msg_no_browser_installed, Toast.LENGTH_LONG).show()
        }
    }

    protected open fun launchMap(address: String?) {
        val encodedAddress = URLEncoder.encode(address)
        //val url = "google.navigation:q=" + encodedAddress
        val url = "https://www.google.com/maps/search/?api=1&query=$encodedAddress"
        launchURL(url)
    }

    fun dialPhone(phoneNumber: String?) {
        if (phoneNumber == null) return
        val url = "tel:$phoneNumber"
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_DIAL, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.message?.let { showAlert(it) }
        }
    }

    fun sendEmail(to: String?) {
        if (to == null) return
        val url = "mailto:$to"
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.message?.let { showAlert(it) }
        }
    }

    /** template method that should be overridden in derived activities that want pull-to-refresh */
    fun onReload() {
    }

    fun logout() {
        Log.d(TAG, "[auth] logout")
        val account = App.getAccount()
        AccountUtils.invalidateAuthToken(this, account.authToken)
        AccountUtils.clearPassword(this, account.username)
        account.clearAuthToken()
    }

    companion object {

        private val TAG = BaseActivity::class.java.simpleName

        fun getAppVersionCode(context: Context): String {
            var version = ""
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                version = String.format("%d", pInfo.versionCode)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("Log", "caught", e)
            }

            return version
        }

        fun activityForNotificationType(notification: PushNotification): Class<out BaseActivity> {
            return when (notification.type) {
                NotificationType.CHECKOUTS -> CheckoutsActivity::class.java
                NotificationType.FINES -> FinesActivity::class.java
                NotificationType.HOLDS -> HoldsActivity::class.java
                NotificationType.PMC -> MessagesActivity::class.java
                NotificationType.GENERAL -> MainActivity::class.java
            }
        }
    }
}
