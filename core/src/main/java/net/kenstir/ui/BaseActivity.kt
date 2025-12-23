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

package net.kenstir.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.account.AccountUtils
import net.kenstir.ui.pn.NotificationType
import net.kenstir.ui.pn.PushNotification
import net.kenstir.ui.util.BusyOverlay
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.ThemeManager
import net.kenstir.ui.util.launchURL
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.BarcodeActivity
import net.kenstir.ui.view.FinesActivity
import net.kenstir.ui.view.MenuProvider
import net.kenstir.ui.view.OrgDetailsActivity
import net.kenstir.ui.view.bookbags.BookBagsActivity
import net.kenstir.ui.view.checkouts.CheckoutsActivity
import net.kenstir.ui.view.holds.HoldsActivity
import net.kenstir.ui.view.main.MainActivity
import net.kenstir.ui.view.messages.MessagesActivity
import net.kenstir.ui.view.search.SearchActivity
import net.kenstir.util.Analytics
import org.evergreen_ils.system.EgOrg
import java.net.URLEncoder

/* Activity base class to handle common behaviours like the navigation drawer */
open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected var toolbar: Toolbar? = null
    protected var appBarLayout: AppBarLayout? = null
    protected var navView: NavigationView? = null
    protected var mainContentView: View? = null
    protected var menuItemHandler: MenuProvider? = null
    protected var progress: ProgressDialogSupport? = null
    protected val busy: BusyOverlay = BusyOverlay(this)
    protected var isRestarting = false
    val scope = lifecycleScope

    protected val feedbackUrl: String
        @SuppressLint("StringFormatInvalid")
        get() {
            val urlFormat = getString(R.string.ou_feedback_url)
            return if (urlFormat.isEmpty()) urlFormat else String.format(urlFormat, getAppVersionCode(this))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.d(TAG, "[init] BaseActivity onCreate")
        super.onCreate(savedInstanceState)

        if (!App.isStarted()) {
            App.restartApp(this)
            isRestarting = true
            return
        }
        isRestarting = false

        Analytics.initialize(this)
        App.init(this)

        initMenuProvider()
        menuItemHandler?.onCreate(this)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        toolbar = findViewById(R.id.toolbar)
        appBarLayout = findViewById(R.id.app_bar_layout)
        navView = findViewById(R.id.nav_view)
        mainContentView = findViewById(R.id.main_content_view)

        setSupportActionBar(toolbar)
    }

    /** Set up the window insets listener to adjust padding for system bars */
    open fun adjustPaddingForEdgeToEdge() {
        //val rootLayout = findViewById<View>(R.id.root_layout)
        val rootLayout = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val recyclerView = findViewById<View>(R.id.recycler_view)
        val listView = findViewById<View>(R.id.list_view)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply top inset to AppBarLayout so Toolbar sits below status bar
            appBarLayout?.updatePadding(top = sysBars.top)

            // Apply bottom inset to content layout
            mainContentView?.updatePadding(bottom = sysBars.bottom)

            // Apply insets to navigation drawer
            navView?.updatePadding(top = sysBars.top, bottom = sysBars.bottom)

            // Dang, this is always null for Search (it hasn't be swapped in yet)
            recyclerView?.updatePadding(bottom = sysBars.bottom)

            // Apply bottom inset to ListView if it exists
            listView?.updatePadding(bottom = sysBars.bottom)

            //WindowInsetsCompat.CONSUMED
            insets
        }
    }

    /** Set up the action bar with a title and subtitle */
    open fun setupActionBar(titleOverride: String? = null, isMainActivity: Boolean = false) {
        val actionBar = supportActionBar
        Log.d(TAG, "[tb] title=$titleOverride main=$isMainActivity actionBar=$actionBar")
        if (actionBar == null) return
        val username =
            if (getResources().getBoolean(R.bool.admin_screenshot_mode)) "janejetson" else App.getAccount().displayName
        actionBar.subtitle = String.format(getString(R.string.ou_activity_subtitle),
            AppState.getString(AppState.LIBRARY_NAME), username)
        titleOverride?.let { actionBar.title = it }
        if (true || !isMainActivity) {
            Log.d(TAG, "[tb] setHomeAsUpEnabled")
            actionBar.setDisplayHomeAsUpEnabled(true)
            //actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            actionBar.setHomeAsUpIndicator(0)
        }
    }

    /** Set up the navigation drawer, if present in this layout */
    open fun setupNavigationDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null) {
            val toggle = ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            toggle.syncState()
        }

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this)
            val navHeader = navigationView.getHeaderView(0)
            navHeader?.let { header ->
                ViewCompat.setOnApplyWindowInsetsListener(header) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updatePadding(top = insets.top) // Apply top padding to the header
                    WindowInsetsCompat.CONSUMED
                }
            }
            navHeader?.setOnClickListener { v -> onNavigationAction(v.id) }
            if (!resources.getBoolean(R.bool.ou_enable_events_button)) {
                navigationView.menu.removeItem(R.id.main_events_button)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun initMenuProvider() {
        menuItemHandler = MenuProvider.create(getString(R.string.ou_menu_provider))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_feedback -> {
                Analytics.logEvent(Analytics.Event.OTHER_ACTION, bundleOf(
                    Analytics.Param.ACTION_NAME to "feedback",
                ))
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

    fun onNavigationAction(id: Int): Boolean {
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
        when (id) {
            R.id.action_switch_account -> {
                Analytics.logEvent(Analytics.Event.ACCOUNT_SWITCH)
                App.restartApp(this)
                return true
            }
            R.id.action_add_account -> {
                Analytics.logEvent(Analytics.Event.ACCOUNT_ADD)
                invalidateOptionsMenu()
                withAsyncBusy { addAccountAndRestart() }
                return true
            }
            R.id.action_clear_all_accounts -> {
                Analytics.logEvent(Analytics.Event.ACCOUNT_LOGOUT)
                maybeLogoutAndClearAccounts()
                return true
            }
            R.id.action_logout -> {
                Analytics.logEvent(Analytics.Event.ACCOUNT_LOGOUT)
                withAsyncBusy { logoutAndRestart() }
                return true
            }
            R.id.action_messages -> {
                Analytics.logEvent(Analytics.Event.MESSAGES_OPEN)
                startActivityForResult(Intent(this, MessagesActivity::class.java), App.REQUEST_MESSAGES)
                return true
            }
            R.id.action_dark_mode -> {
                ThemeManager.saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                // return false or else the menu view will be leaked when the activity is restarted
                return false
            }
            R.id.action_light_mode -> {
                ThemeManager.saveAndApplyNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                // return false or else the menu view will be leaked when the activity is restarted
                return false
            }
            else -> return false
        }
    }

    suspend fun addAccountAndRestart() {
        try {
            val bnd = AccountUtils.addAccount(this@BaseActivity)
            Log.d(TAG, "[auth] addAccountAndRestart: added account, bnd=$bnd")
            // TODO: restartAppWithNewAccount(bnd.getString(AccountManager.KEY_ACCOUNT_NAME))
            App.restartApp(this@BaseActivity)
        } catch (_: android.accounts.OperationCanceledException) {
            // user cancelled, do nothing
//        } catch (ex: Exception) {
//            Log.d(TAG, "[auth] addAccountAndRestart: caught", ex)
//            showAlert(ex)
        }
    }

    suspend fun logoutAndRestart() {
        logout()
        App.restartApp(this@BaseActivity)
    }

    private fun maybeLogoutAndClearAccounts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            showAlert(getString(R.string.android_too_old_message))
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.clear_accounts_alert_title)
            .setMessage(R.string.clear_accounts_alert_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> withAsyncBusy { logoutAndClearAccounts() } }
            .setNegativeButton(android.R.string.cancel, null)
        builder.create().show()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private suspend fun logoutAndClearAccounts() {
        logout()
        AccountUtils.removeAllAccounts(this@BaseActivity)
        App.restartApp(this@BaseActivity)
    }

    suspend fun logout() {
        Log.d(TAG, "[auth] logout")
        val account = App.getAccount()
        App.getServiceConfig().userService.deleteSession(account)
        AccountUtils.invalidateAuthToken(this, account.authToken)
        AccountUtils.clearPassword(this, account.username)
        account.clearAuthToken()
    }

    open fun launchMap(address: String?) {
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
        val uri = url.toUri()
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.message?.let { showAlert(it) }
        }
    }

    suspend fun withBusy(msg: String, block: suspend () -> Unit) {
        try {
            busy.showOverlay(msg)
            yield() // allow UI to render before starting work
            block()
        } catch (ex: Exception) {
            Log.d(TAG, "[busy] caught", ex)
            showAlert(ex)
        } finally {
            busy.hideOverlay()
        }
    }

    fun withAsyncBusy(msg: String = "", block: suspend () -> Unit) {
        scope.async {
            try {
                busy.showOverlay(msg)
                yield() // allow UI to render before starting work
                block()
            } catch (ex: Exception) {
                Log.d(TAG, "[busy] caught", ex)
                showAlert(ex)
            } finally {
                busy.hideOverlay()
            }
        }
    }

    /** template method that should be overridden in derived activities that want pull-to-refresh */
    fun onReload() {
    }

    companion object {
        private const val TAG = "BaseActivity"

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
