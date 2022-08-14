/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.searchCatalog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.Analytics.orgDimensionKey
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.utils.getCustomMessage
import org.evergreen_ils.utils.ui.*
import org.evergreen_ils.views.bookbags.BookBagUtils.showAddToListDialog
import org.evergreen_ils.views.holds.PlaceHoldActivity
import org.opensrf.util.OSRFObject

class SearchActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private val TAG = SearchActivity::class.java.simpleName
    private val SEARCH_OPTIONS_VISIBLE = "search_options_visible"

    private var searchTextView: EditText? = null
    private var searchOptionsButton: SwitchCompat? = null
    private var searchOptionsLayout: View? = null
    private var searchButton: Button? = null
    private var orgSpinner: Spinner? = null
    private var searchClassSpinner: Spinner? = null
    private var searchFormatSpinner: Spinner? = null
    private var searchResultsSummary: TextView? = null
    private var searchResultsFragment: SearchResultsFragment? = null
    private var progress: ProgressDialogSupport? = null
    private var haveSearched = false
    private var contextMenuRecordInfo: ContextMenuRecordInfo? = null

    private val searchText: String
        get() = searchTextView?.text.toString().trim()

    private val searchClass: String
        get() = searchClassSpinner?.selectedItem.toString().toLowerCase()

    private val searchFormatCode: String?
        get() = EgCodedValueMap.searchFormatCode(searchFormatSpinner?.selectedItem.toString())

    private val accountIdKey = "accountId"

    private class ContextMenuRecordInfo : ContextMenu.ContextMenuInfo {
        var record: MBRecord? = null
        var position = 0
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_search)

        progress = ProgressDialogSupport()

        // clear prior search results unless this is the same user and we just rotated
        val lastAccountId = savedInstanceState?.getInt(accountIdKey)
        val accountId = App.getAccount().id ?: -1
        Log.d(TAG, "lastAccountId = $lastAccountId")
        Log.d(TAG, "accountId = $accountId")
        if (lastAccountId == null || lastAccountId != accountId) {
            clearResults()
        }

        // create search results fragment
        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            searchResultsFragment = SearchResultsFragment()
            transaction.replace(R.id.search_results_list, searchResultsFragment!!)
            transaction.commit()
        } else {
            searchResultsFragment = supportFragmentManager.findFragmentById(R.id.search_results_list) as SearchResultsFragment?
        }

        searchTextView = findViewById(R.id.searchText)
        searchOptionsButton = findViewById(R.id.search_options_button)
        searchOptionsLayout = findViewById(R.id.search_options_layout)
        searchButton = findViewById(R.id.search_button)
        searchClassSpinner = findViewById(R.id.search_qtype_spinner)
        searchFormatSpinner = findViewById(R.id.search_format_spinner)
        orgSpinner = findViewById(R.id.search_org_spinner)
        searchResultsSummary = findViewById(R.id.search_result_number)

        initSearchOptionsVisibility()
        initSearchText()
        initSearchOptionsButton()
        initSearchButton()
        initSearchClassSpinner()
        initSearchFormatSpinner()
        initOrgSpinner()
        initRecordClickListener()
        updateSearchResultsSummary()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        App.getAccount().id?.let { id ->
            outState.putInt(accountIdKey, id)
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)
        fetchData()
    }

    private fun clearResults() {
        haveSearched = false
        EgSearch.clearResults()
    }

    private fun initSearchButton() {
        searchButton?.setOnClickListener { fetchSearchResults() }
    }

    private fun initSearchOptionsVisibility() {
        val lastState = AppState.getBoolean(SEARCH_OPTIONS_VISIBLE, true)
        searchOptionsButton?.isChecked = lastState
        setSearchOptionsVisibility(lastState)
    }

    private fun setSearchOptionsVisibility(visible: Boolean) {
        searchOptionsLayout?.visibility = if (visible) View.VISIBLE else View.GONE
        AppState.setBoolean(SEARCH_OPTIONS_VISIBLE, visible)
    }

    private fun initSearchOptionsButton() {
        searchOptionsButton?.setOnCheckedChangeListener { buttonView, isChecked -> setSearchOptionsVisibility(isChecked) }
    }

    private fun initSearchText() {
        searchTextView?.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                fetchSearchResults()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun fetchData() {
        async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()

                // fetch bookbags
                when (val result = GatewayLoader.loadBookBagsAsync(App.getAccount())) {
                    is Result.Success -> {}
                    is Result.Error -> { showAlert(result.exception); return@async }
                }

                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            }
        }
    }

    private fun fetchSearchResults() {
        async {
            try {
                val start = System.currentTimeMillis()
                //var jobs = mutableListOf<Job>()
                progress?.show(this@SearchActivity, getString(R.string.dialog_fetching_data_message))

                Log.d(TAG, "[kcxxx] fetchSearchResults ...")

                // check searchText is not blank
                if (searchText.isBlank()) {
                    searchTextView?.error = getString(R.string.msg_search_words_required)
                    return@async
                }

                // hide soft keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchTextView?.windowToken, 0)

                // query returns a list of IDs
                val queryString = EgSearch.makeQueryString(searchText, searchClass, searchFormatCode, getString(R.string.ou_sort_by))
                val result = Gateway.search.fetchMulticlassQuery(queryString, EgSearch.searchLimit)
                when (result) {
                    is Result.Success -> {
                        haveSearched = true
                        EgSearch.loadResults(result.data)
                        logSearchEvent(result)
                    }
                    is Result.Error -> {
                        showAlert(result.exception)
                        logSearchEvent(result)
                        return@async
                    }
                }
                updateSearchResultsSummary()
                searchResultsFragment?.notifyDatasetChanged()

                //jobs.joinAll()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchSearchResults ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchSearchResults ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun logSearchEvent(result: Result<OSRFObject>) {
        val b = bundleOf(
                Analytics.Param.SEARCH_TERM to searchText,
                Analytics.Param.SEARCH_CLASS to searchClass,
                Analytics.Param.SEARCH_FORMAT to searchFormatCode,
                Analytics.Param.SEARCH_ORG_KEY to
                        orgDimensionKey(EgSearch.selectedOrganization,
                                EgOrg.findOrg(App.getAccount().searchOrg),
                                EgOrg.findOrg(App.getAccount().homeOrg)),
        )
        when (result) {
            is Result.Success -> {
                b.putString(Analytics.Param.RESULT, Analytics.Value.OK)
                b.putInt(Analytics.Param.NUM_RESULTS, EgSearch.visible)
            }
            is Result.Error ->
                b.putString(Analytics.Param.RESULT, result.exception.getCustomMessage())
        }
        Analytics.logEvent(Analytics.Event.SEARCH, b)
    }

    private fun updateSearchResultsSummary() {
        var s: String? = null
        val size = EgSearch.results.size
        if (size < EgSearch.visible) {
            s = getString(R.string.first_n_of_m_results, size, EgSearch.visible)
        } else if (size == 0 && haveSearched) {
            s = getString(R.string.no_results)
        } else if (size > 0 || haveSearched) {
            s = getString(R.string.n_results, EgSearch.visible)
        }
        searchResultsSummary?.text = s
    }

    private fun initOrgSpinner() {
        var selectedOrgPos = 0
        val defaultOrgId = App.getAccount().searchOrg
        val list = ArrayList<String>()
        for ((index, org) in EgOrg.visibleOrgs.withIndex()) {
            list.add(org.spinnerLabel)
            if (org.id == defaultOrgId) {
                selectedOrgPos = index
            }
        }
        val adapter: ArrayAdapter<String> = OrgArrayAdapter(this, R.layout.org_item_layout, list, false)
        orgSpinner?.adapter = adapter
        orgSpinner?.setSelection(selectedOrgPos)
        EgSearch.selectedOrganization = EgOrg.visibleOrgs[selectedOrgPos]
        orgSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                EgSearch.selectedOrganization = EgOrg.visibleOrgs[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun initSearchFormatSpinner() {
        val labels = EgCodedValueMap.searchFormatSpinnerLabels
        val adapter = ArrayAdapter(this, R.layout.org_item_layout, labels)
        searchFormatSpinner?.adapter = adapter
    }

    private fun initSearchClassSpinner() {
        searchClassSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "position: $position")
                if (position == 5) {
                    startScanning()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "here")
            }
        }
    }

    private fun initRecordClickListener() {
        registerForContextMenu(findViewById(R.id.search_results_list))
        searchResultsFragment?.setOnRecordClickListener { record, position ->
            val intent = Intent(baseContext, RecordDetailsActivity::class.java)
            intent.putExtra("orgID", EgSearch.selectedOrganization?.id)
            intent.putExtra("recordPosition", position)
            intent.putExtra("numResults", EgSearch.visible)
            startActivityForResult(intent, 10)
        }
        searchResultsFragment?.setOnRecordLongClickListener { record, position ->
            contextMenuRecordInfo = ContextMenuRecordInfo()
            contextMenuRecordInfo?.record = record
            contextMenuRecordInfo?.position = position
            openContextMenu(findViewById(R.id.search_results_list))
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (v.id == R.id.search_results_list) {
//            val info = menuInfo as AdapterView.AdapterContextMenuInfo
            menu.add(Menu.NONE, App.ITEM_SHOW_DETAILS, 0, getString(R.string.show_details_message))
            menu.add(Menu.NONE, App.ITEM_PLACE_HOLD, 1, getString(R.string.button_place_hold))
            menu.add(Menu.NONE, App.ITEM_ADD_TO_LIST, 2, getString(R.string.add_to_my_list_message))
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        //ContextMenuRecordInfo info = (ContextMenuRecordInfo)item.getMenuInfo();
        val info = contextMenuRecordInfo ?: return false
        when (item.itemId) {
            App.ITEM_SHOW_DETAILS -> {
                val intent = Intent(baseContext, RecordDetailsActivity::class.java)
                intent.putExtra("orgID", EgSearch.selectedOrganization?.id)
                intent.putExtra("recordPosition", info.position)
                intent.putExtra("numResults", EgSearch.visible)
                startActivity(intent)
                return true
            }
            App.ITEM_PLACE_HOLD -> {
                val intent = Intent(baseContext, PlaceHoldActivity::class.java)
                intent.putExtra("recordInfo", info.record)
                startActivity(intent)
                return true
            }
            App.ITEM_ADD_TO_LIST -> {
                if (!App.getAccount().bookBags.isNullOrEmpty()) {
                    //Analytics.logEvent("lists_additem", "via", "results_long_press")
                    showAddToListDialog(this, App.getAccount().bookBags, info.record!!)
                } else {
                    Toast.makeText(this, getText(R.string.msg_no_lists), Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    //// TODO: 4/30/2017 pull up
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        val url = feedbackUrl
        if (TextUtils.isEmpty(url)) menu.removeItem(R.id.action_feedback)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_advanced_search) {
            startActivityForResult(Intent(applicationContext, AdvancedSearchActivity::class.java), 2)
            return true
        } else if (id == R.id.action_logout) {
            Analytics.logEvent(Analytics.Event.ACCOUNT_LOGOUT)
            logout()
            App.restartApp(this)
            return true
        } else if (id == R.id.action_barcode_search) {
            startScanning()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RecordDetailsActivity.RETURN_DATA -> {
            }
            AdvancedSearchActivity.RESULT_ADVANCED_SEARCH -> {
                Log.d(TAG, "result text:" + data!!.getStringExtra("advancedSearchText"))
                searchTextView?.setText(data.getStringExtra("advancedSearchText"))
                fetchSearchResults()
            }
//            CaptureActivity.BARCODE_SEARCH -> {
//                searchTextView?.setText("identifier|isbn: "
//                        + data?.getStringExtra("barcodeValue"))
//                fetchSearchResults()
//            }
        }
    }

    fun startScanning() {
        val scanner = GmsBarcodeScanning.getClient(this)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                handleBarcodeResult(barcode)
            }
            .addOnCanceledListener {
                //this.showAlert("cancelled")
            }
            .addOnFailureListener { e ->
                this.showAlert(e)
            }

    }

    private fun handleBarcodeResult(barcode: Barcode) {
        when(barcode.valueType) {
            Barcode.TYPE_ISBN -> {}
            Barcode.TYPE_PRODUCT -> {}
            else -> {
                showAlert("Only ISBN or UPC barcodes are supported")
                return
            }
        }
        barcode.rawValue?.let {
            searchTextView?.setText("identifier:$it")
            fetchSearchResults()
        }
    }
}
