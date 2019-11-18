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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.evergreen_ils.Api
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.accountAccess.AccountUtils
import org.evergreen_ils.accountAccess.bookbags.BookBagActivity
import org.evergreen_ils.accountAccess.checkout.CheckoutsActivity
import org.evergreen_ils.accountAccess.fines.FinesActivity
import org.evergreen_ils.accountAccess.holds.HoldsActivity
import org.evergreen_ils.android.App
import org.evergreen_ils.android.App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.searchCatalog.SearchActivity
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.EvergreenServer
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.Utils
import org.evergreen_ils.utils.ui.ThemeManager
import org.evergreen_ils.views.BarcodeActivity
import org.evergreen_ils.views.MainActivity
import org.evergreen_ils.views.MenuProvider
import org.evergreen_ils.views.splashscreen.SplashActivity
import org.opensrf.util.GatewayResponse
import java.net.URLEncoder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/* Activity base class to handle common behaviours like the navigation drawer */
open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CoroutineScope {

    private var _toolbar: Toolbar? = null
    protected var menuItemHandler: MenuProvider? = null
    protected var isRestarting = false
    protected lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

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
        ThemeManager.applyNightMode()

        job = Job()

        initMenuProvider()
        menuItemHandler?.onCreate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "coro: cancel")
        job.cancel()
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

    internal fun initMenuProvider() {
        menuItemHandler = MenuProvider.create(getString(R.string.ou_menu_provider))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_feedback) {
            Analytics.logEvent("Feedback: Open")
            val url = feedbackUrl
            if (!TextUtils.isEmpty(url)) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return onNavigationAction(id)
    }

    protected fun onNavigationAction(id: Int): Boolean {
        var ret = true
        if (id == R.id.nav_header) {
            Analytics.logEvent("Home: Open", "via", "nav_drawer")
            startActivity(Intent(this, MainActivity::class.java))
        } else if (id == R.id.main_btn_search) {
            Analytics.logEvent("Search: Open", "via", "nav_drawer")
            startActivity(Intent(this, SearchActivity::class.java))
        } else if (id == R.id.account_btn_check_out) {
            Analytics.logEvent("Checkouts: Open", "via", "nav_drawer")
            startActivity(Intent(this, CheckoutsActivity::class.java))
        } else if (id == R.id.account_btn_holds) {
            Analytics.logEvent("Holds: Open", "via", "nav_drawer")
            startActivity(Intent(this, HoldsActivity::class.java))
        } else if (id == R.id.account_btn_fines) {
            Analytics.logEvent("Fines: Open", "via", "nav_drawer")
            startActivity(Intent(this, FinesActivity::class.java))
        } else if (id == R.id.main_my_lists_button) {
            Analytics.logEvent("Lists: Open", "via", "nav_drawer")
            startActivity(Intent(this, BookBagActivity::class.java))
        } else if (id == R.id.btn_barcode) {
            Analytics.logEvent("Barcode: Open", "via", "nav_drawer")
            // generating via Intent only works if zxing barcode app is installed
            //            Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
            //            intent.putExtra("ENCODE_FORMAT", "CODABAR");
            //            intent.putExtra("ENCODE_DATA", "12345678901234");
            //            startActivity(intent);
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
            Analytics.logEvent("Account: Switch Account", "via", "options_menu")
            App.restartApp(this)
            return true
        } else if (id == R.id.action_add_account) {
            Analytics.logEvent("Account: Add Account", "via", "options_menu")
            invalidateOptionsMenu()
            AccountUtils.addAccount(this) { App.restartApp(this@BaseActivity) }
            return true
        } else if (id == R.id.action_logout) {
            Analytics.logEvent("Account: Logout", "via", "options_menu")
            AccountAccess.getInstance().logout(this)
            App.restartApp(this)
            return true
            //        } else if (id == R.id.action_feedback) {
            //            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getFeedbackUrl())));
            //            return true;
        } else if (id == R.id.action_messages) {
            Analytics.logEvent("Messages: Open", "via", "options_menu")
            val username = AccountAccess.getInstance().userName
            val password = AccountUtils.getPassword(this, username)
            var path = ("/eg/opac/login"
                    + "?redirect_to=" + URLEncoder.encode("/eg/opac/myopac/messages"))
            if (!TextUtils.isEmpty(username))
                path = path + "&username=" + URLEncoder.encode(username)
            if (!TextUtils.isEmpty(password))
                path = path + "&password=" + URLEncoder.encode(password)
            val url = EvergreenServer.getInstance().getUrl(path)
            startActivityForResult(Intent(Intent.ACTION_VIEW, Uri.parse(url)), REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT)
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
    }
}