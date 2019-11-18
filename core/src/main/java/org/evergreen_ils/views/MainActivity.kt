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
import androidx.core.view.MenuItemCompat
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import kotlinx.coroutines.launch
import org.evergreen_ils.Api

import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountUtils
import org.evergreen_ils.accountAccess.bookbags.BookBagActivity
import org.evergreen_ils.accountAccess.checkout.CheckoutsActivity
import org.evergreen_ils.accountAccess.fines.FinesActivity
import org.evergreen_ils.accountAccess.holds.HoldsActivity
import org.evergreen_ils.android.App
import org.evergreen_ils.net.GatewayJsonObjectRequest
import org.evergreen_ils.net.VolleyWrangler
import org.evergreen_ils.searchCatalog.SearchActivity
import org.evergreen_ils.system.*
import org.evergreen_ils.utils.ui.BaseActivity
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : BaseActivity() {

    private var mUnreadMessageCount: Int? = null //unknown
    private var mUnreadMessageText: TextView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        Log.d(TAG, "coro: onCreate")

        setContentView(R.layout.activity_main)

        EvergreenServerLoader.fetchOrgSettings(this)
        EvergreenServerLoader.fetchSMSCarriers(this)
        fetchUnreadMessageCount()
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "coro: onDestroy")
    }

    public override fun onPause() {
        super.onPause()
        Log.d(TAG, "coro: onPause")
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "coro: onResume")
        Log.d(TAG, "coro: launch")
        launch {
            val response = getData()
            Log.d(TAG, "coro: main? resp:$response")
        }
    }

    private suspend fun getData() = suspendCoroutine<String> { cont ->
        val url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                Api.ACTOR, Api.ILS_VERSION,
                arrayOf()))
        val start_ms = System.currentTimeMillis()
        val r = GatewayJsonObjectRequest(
                url,
                Request.Priority.NORMAL,
                Response.Listener { response ->
                    val duration_ms = System.currentTimeMillis() - start_ms
                    val ver = response.payload as String
                    Log.d(TAG, "coro: listener, resp:$ver")
                    cont.resumeWith(Result.success(ver))
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, "caught", error)
                    cont.resumeWithException(error)
                })
        VolleyWrangler.getInstance(this).addToRequestQueue(r)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult req=$requestCode result=$resultCode")
        if (requestCode == App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT) {
            fetchUnreadMessageCount()
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
        updateUnreadMessageText()
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

    private fun updateUnreadMessageText() {
        if (mUnreadMessageText == null)
            return
        if (isFinishing)
            return
        if (mUnreadMessageCount != null) {
            mUnreadMessageText?.visibility = if (mUnreadMessageCount!! > 0) View.VISIBLE else View.GONE;
            mUnreadMessageText!!.setText(String.format("%d", mUnreadMessageCount))
        } else {
            mUnreadMessageText?.visibility = View.GONE
        }
    }

    private fun fetchUnreadMessageCount() {
        if (!resources.getBoolean(R.bool.ou_enable_messages))
            return
        EvergreenServerLoader.fetchUnreadMessageCount(this) { count: Int? ->
            mUnreadMessageCount = count
            updateUnreadMessageText()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (menuItemHandler?.onItemSelected(this, id, "main_option_menu") == true)
            return true
        return if (handleMenuAction(id)) true else super.onOptionsItemSelected(item)
    }

    fun onButtonClick(v: View) {
        val id = v.id
        if (id == R.id.account_btn_check_out) {
            Analytics.logEvent("Checkouts: Open", "via", "main_button")
            startActivity(Intent(this, CheckoutsActivity::class.java))
        } else if (id == R.id.account_btn_holds) {
            Analytics.logEvent("Holds: Open", "via", "main_button")
            startActivity(Intent(this, HoldsActivity::class.java))
        } else if (id == R.id.account_btn_fines) {
            Analytics.logEvent("Fines: Open", "via", "main_button")
            startActivity(Intent(this, FinesActivity::class.java))
        } else if (id == R.id.main_my_lists_button) {
            Analytics.logEvent("Lists: Open", "via", "main_button")
            startActivity(Intent(this, BookBagActivity::class.java))
        } else if (id == R.id.main_btn_search) {
            Analytics.logEvent("Search: Open", "via", "main_button")
            startActivity(Intent(this, SearchActivity::class.java))
        } else if (id == R.id.main_barcode_button) {
            Analytics.logEvent("Barcode: Open", "via", "main_button")
            startActivity(Intent(this, BarcodeActivity::class.java))
        } else if (menuItemHandler != null) {
            menuItemHandler?.onItemSelected(this, id, "main_button")
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
    }
}