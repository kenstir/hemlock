/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
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
package net.kenstir.ui.view.search

import android.content.Intent
import android.content.pm.PackageInfo
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
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.bundleOf
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.*
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_READY_TO_DOWNLOAD
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.util.Analytics
import net.kenstir.util.Analytics.orgDimensionKey
import net.kenstir.ui.Key
import net.kenstir.logging.Log
import net.kenstir.ui.util.showAlert
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.SearchClass
import net.kenstir.data.service.SearchResults
import net.kenstir.ui.App
import net.kenstir.ui.AppState
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.OrgArrayAdapter
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.SpinnerStringOption
import net.kenstir.ui.util.compatEnableEdgeToEdge
import org.evergreen_ils.system.EgCodedValueMap
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSearch
import net.kenstir.util.getCustomMessage
import net.kenstir.ui.view.bookbags.BookBagUtils.showAddToListDialog
import net.kenstir.ui.view.holds.PlaceHoldActivity

const val ITEM_PLACE_HOLD = 0
const val ITEM_SHOW_DETAILS = 1
const val ITEM_ADD_TO_LIST = 2

class SearchActivity : BaseActivity() {
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
    private var searchResults: SearchResults? = null
    private var contextMenuRecordInfo: ContextMenuRecordInfo? = null
    private lateinit var searchClassOption: SpinnerStringOption
    private lateinit var searchFormatOption: SpinnerStringOption
    private lateinit var searchOrgOption: SpinnerStringOption

    private val searchText: String
        get() = searchTextView?.text.toString().trim()
    private val searchClass: String
        get() = searchClassOption.value
    private val searchFormatCode: String
        get() = searchFormatOption.value

    private class ContextMenuRecordInfo : ContextMenu.ContextMenuInfo {
        var record: BibRecord? = null
        var position = 0
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_search)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        progress = ProgressDialogSupport()

        // clear prior search results unless this is the same user and we just rotated
        val lastAccountId = savedInstanceState?.getInt(Key.ACCOUNT_ID)
        val accountId = App.getAccount().id
        Log.d(TAG, "lastAccountId = $lastAccountId")
        Log.d(TAG, "accountId = $accountId")
        if (lastAccountId == null || lastAccountId != accountId) {
            clearResults()
        } else {
            restoreResults()
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
        searchClassSpinner = findViewById(R.id.search_class_spinner)
        searchFormatSpinner = findViewById(R.id.search_format_spinner)
        orgSpinner = findViewById(R.id.search_org_spinner)
        searchResultsSummary = findViewById(R.id.search_result_number)

        initSearchOptions()
        initSearchOptionsVisibility()
        initSearchText()
        initSearchButton()
        initSearchClassSpinner()
        initSearchFormatSpinner()
        initOrgSpinner()
        initRecordClickListener()
        updateSearchResultsSummary()
        doSearchOnStartup(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        App.getAccount().id?.let { id ->
            outState.putInt(Key.ACCOUNT_ID, id)
        }
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    private fun clearResults() {
        haveSearched = false
        searchResults = null
    }

    private fun restoreResults() {
        haveSearched = true
        searchResults = App.getServiceConfig().searchService.getLastSearchResults()
    }

    private fun initSearchButton() {
        searchButton?.setOnClickListener { fetchSearchResults() }
    }

    private fun initSearchOptions() {
        searchClassOption = SpinnerStringOption(
            key = AppState.SEARCH_CLASS,
            defaultValue = SearchClass.KEYWORD,
            optionLabels = SearchClass.spinnerLabels,
            optionValues = SearchClass.spinnerValues
        )
        searchFormatOption = SpinnerStringOption(
            key = AppState.SEARCH_FORMAT,
            defaultValue = "",
            optionLabels = EgCodedValueMap.searchFormatSpinnerLabels,
            optionValues = EgCodedValueMap.searchFormatSpinnerValues
        )
        searchOrgOption = SpinnerStringOption(
            key = AppState.SEARCH_ORG_SHORT_NAME,
            defaultValue = EgOrg.findOrg(App.getAccount()?.searchOrg)?.shortname ?: EgOrg.visibleOrgs[0].shortname,
            optionLabels = EgOrg.orgSpinnerLabels(),
            optionValues = EgOrg.spinnerShortNames()
        )
    }

    private fun initSearchOptionsVisibility() {
        val lastState = AppState.getBoolean(AppState.SEARCH_OPTIONS_ARE_VISIBLE, true)
        searchOptionsButton?.isChecked = lastState
        setSearchOptionsVisibility(lastState)
        searchOptionsButton?.setOnCheckedChangeListener { _, isChecked -> setSearchOptionsVisibility(isChecked) }
    }

    private fun setSearchOptionsVisibility(visible: Boolean) {
        searchOptionsLayout?.visibility = if (visible) View.VISIBLE else View.GONE
        AppState.setBoolean(AppState.SEARCH_OPTIONS_ARE_VISIBLE, visible)
    }

    private fun initSearchText() {
        searchTextView?.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                fetchSearchResults()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[fetch] fetchData ...")
                val start = System.currentTimeMillis()

                // load bookbags
                val result = App.getServiceConfig().userService.loadPatronLists(App.getAccount())
                when (result) {
                    is Result.Success -> {}
                    is Result.Error -> { showAlert(result.exception); return@async }
                }

                Log.logElapsedTime(TAG, start, "[fetch] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchData ... caught", ex)
                showAlert(ex)
            }
        }
    }

    private fun fetchSearchResults() {
        scope.async {
            try {
                val start = System.currentTimeMillis()
                progress?.show(this@SearchActivity, getString(R.string.dialog_fetching_data_message))

                // check searchText is not blank
                if (searchText.isBlank()) {
                    searchTextView?.error = getString(R.string.msg_search_words_required)
                    return@async
                }

                // hide soft keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchTextView?.windowToken, 0)

                // submit the query
                val queryString = App.getServiceConfig().searchService.makeQueryString(searchText, searchClass, searchFormatCode, getString(R.string.ou_sort_by))
                Log.d(TAG, "[fetch] fetchSearchResults ... \"$queryString\"")
                val result = App.getServiceConfig().searchService.searchCatalog(queryString, resources.getInteger(R.integer.ou_search_limit))
                when (result) {
                    is Result.Success -> {
                        haveSearched = true
                        searchResults = result.get()
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

                Log.logElapsedTime(TAG, start, "[fetch] fetchSearchResults ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[fetch] fetchSearchResults ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun logSearchEvent(result: Result<SearchResults>) {
        val b = bundleOf(
                Analytics.Param.SEARCH_CLASS to searchClass,
                Analytics.Param.SEARCH_FORMAT to searchFormatCode,
                Analytics.Param.SEARCH_ORG_KEY to
                        orgDimensionKey(EgSearch.selectedOrganization,
                                EgOrg.findOrg(App.getAccount().searchOrg),
                                EgOrg.findOrg(App.getAccount().homeOrg)),
        )
        b.putAll(Analytics.searchTextStats(searchText))
        when (result) {
            is Result.Success -> {
                b.putString(Analytics.Param.RESULT, Analytics.Value.OK)
                b.putInt(Analytics.Param.NUM_RESULTS, searchResults?.numResults ?: 0)
            }
            is Result.Error ->
                b.putString(Analytics.Param.RESULT, result.exception.getCustomMessage())
        }
        Analytics.logEvent(Analytics.Event.SEARCH, b)
    }

    private fun updateSearchResultsSummary() {
        var s: String? = null
        searchResults?.let {
            val size = it.numResults
            val total = it.totalMatches
            if (size < total) {
                s = getString(R.string.first_n_of_m_results, size, total)
            } else if (size == 0 && haveSearched) {
                s = getString(R.string.no_results)
            } else if (size > 0 || haveSearched) {
                s = getString(R.string.n_results, total)
            }
        }
        searchResultsSummary?.text = s
    }

    private fun initOrgSpinner() {
        // connect spinner to option and set adapter
        val option = searchOrgOption
        option.spinner = orgSpinner
        orgSpinner?.adapter = OrgArrayAdapter(this, R.layout.org_item_layout, option.optionLabels, false)

        // restore last selected value and monitor changes
        option.load()
        EgSearch.selectedOrganization = EgOrg.visibleOrgs[option.selectedIndex]
        option.addSelectionListener { index, value ->
            Log.d(TAG, "[prefs] ${option.key} changed: $index $value")
            EgSearch.selectedOrganization = EgOrg.visibleOrgs[index]
        }
    }

    private fun initSearchFormatSpinner() {
        // connect spinner to option and set adapter
        val option = searchFormatOption
        option.spinner = searchFormatSpinner
        searchFormatSpinner?.adapter = ArrayAdapter(this, R.layout.org_item_layout, option.optionLabels)

        // restore last selected value and monitor changes
        option.load()
        option.addSelectionListener { index, value ->
            Log.d(TAG, "[prefs] ${option.key} changed: $index $value")
        }
    }

    private fun initSearchClassSpinner() {
        // connect spinner to option and set adapter
        val option = searchClassOption
        option.spinner = searchClassSpinner
        searchClassSpinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, option.optionLabels)

        // restore last selected value and monitor changes
        option.load()
        option.addSelectionListener { index, value ->
            Log.d(TAG, "[prefs] ${option.key} changed: $index $value")
        }
    }

    private fun initRecordClickListener() {
        registerForContextMenu(findViewById(R.id.search_results_list))
        searchResultsFragment?.setOnRecordClickListener { _, position ->
            val intent = Intent(baseContext, RecordDetailsActivity::class.java)
            intent.putExtra(Key.ORG_ID, EgSearch.selectedOrganization?.id)
            intent.putExtra(Key.RECORD_POSITION, position)
            intent.putExtra(Key.NUM_RESULTS, searchResults?.numResults ?: 0)
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
            menu.add(Menu.NONE, ITEM_SHOW_DETAILS, 0, getString(R.string.show_details_message))
            menu.add(Menu.NONE, ITEM_PLACE_HOLD, 1, getString(R.string.button_place_hold))
            menu.add(Menu.NONE, ITEM_ADD_TO_LIST, 2, getString(R.string.add_to_my_list_message))
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = contextMenuRecordInfo ?: return super.onContextItemSelected(item)
        when (item.itemId) {
            ITEM_SHOW_DETAILS -> {
                val intent = Intent(baseContext, RecordDetailsActivity::class.java)
                intent.putExtra(Key.ORG_ID, EgSearch.selectedOrganization?.id)
                intent.putExtra(Key.RECORD_POSITION, info.position)
                intent.putExtra(Key.NUM_RESULTS, searchResults?.numResults ?: 0)
                startActivity(intent)
                return true
            }
            ITEM_PLACE_HOLD -> {
                val intent = Intent(baseContext, PlaceHoldActivity::class.java)
                intent.putExtra(Key.RECORD_INFO, info.record)
                startActivity(intent)
                return true
            }
            ITEM_ADD_TO_LIST -> {
                if (App.getAccount().patronLists.isNotEmpty()) {
                    //Analytics.logEvent("lists_additem", "via", "results_long_press")
                    showAddToListDialog(this, App.getAccount().patronLists, info.record!!)
                } else {
                    Toast.makeText(this, getText(R.string.msg_no_lists), Toast.LENGTH_SHORT).show()
                }
                return true
            }
            else ->
                return super.onContextItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        if (TextUtils.isEmpty(feedbackUrl))
            menu.removeItem(R.id.action_feedback)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_advanced_search -> {
                startActivityForResult(Intent(applicationContext, AdvancedSearchActivity::class.java), 2)
                return true
            }
            R.id.action_logout -> {
                Analytics.logEvent(Analytics.Event.ACCOUNT_LOGOUT)
                logout()
                App.restartApp(this)
                return true
            }
            R.id.action_barcode_search -> {
                startScanning()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_CODE_NORMAL -> {
                // noop
            }
            RESULT_CODE_SEARCH_BY_AUTHOR -> {
                // NOTREACHED
                searchTextView?.setText(data?.getStringExtra(Key.SEARCH_TEXT))
                setSearchClass(SearchClass.AUTHOR)
                fetchSearchResults()
            }
            RESULT_CODE_SEARCH_BY_KEYWORD -> {
                searchTextView?.setText(data?.getStringExtra(Key.SEARCH_TEXT))
                fetchSearchResults()
            }
        }
    }

    private fun doSearchOnStartup(data: Intent) {
        val text = data.getStringExtra(Key.SEARCH_TEXT)
        val code = data.getIntExtra(Key.SEARCH_BY, 0)
        if (text?.isNotEmpty() == true && code == RESULT_CODE_SEARCH_BY_AUTHOR) {
            searchTextView?.setText(text)
            setSearchClass(SearchClass.AUTHOR)
            fetchSearchResults()
        }
    }

    private fun startScanning() {
        // determine if the barcode scanning module is installed, and if not, install it now
        // See also https://developers.google.com/android/guides/module-install-apis
        val start = System.currentTimeMillis()
        val moduleInstallClient = ModuleInstall.getClient(this)
        val barcodeScanner = GmsBarcodeScanning.getClient(this)
        moduleInstallClient
            .areModulesAvailable(barcodeScanner)
            .addOnSuccessListener {
                Log.logElapsedTime(TAG, start, "[scanner] module status: ${it.availabilityStatus}")
                if (it.availabilityStatus == STATUS_READY_TO_DOWNLOAD) {
                    this.installScannerModule(moduleInstallClient, barcodeScanner)
                } else if (it.availabilityStatus == STATUS_ALREADY_AVAILABLE) {
                    this.startScanningWithClient(barcodeScanner)
                }
            }
            .addOnFailureListener {
                // this can fail with "17: API: ModuleInstall.API is not available on this device..."
                // and that is a terrible UX.  Calling startScanning still fails, but produces
                // a better error message.
                this.startScanningWithClient(barcodeScanner)
            }

    }

    private fun installScannerModule(moduleInstallClient: ModuleInstallClient, scanner: GmsBarcodeScanner) {
        val start = System.currentTimeMillis()
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()
        progress?.show(this@SearchActivity, getString(R.string.msg_installing_scanner_module))
        moduleInstallClient.installModules(moduleInstallRequest)
            .addOnSuccessListener {
                this.startScanningWithClient(scanner)
            }
            .addOnFailureListener {
                this.onScannerFailure(it)
            }
            .addOnCompleteListener {
                progress?.dismiss()
                Log.logElapsedTime(TAG, start, "[scanner] module install done")
            }
    }

    private fun startScanningWithClient(scanner: GmsBarcodeScanner) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                Analytics.logEvent(Analytics.Event.SCAN, bundleOf(Analytics.Param.RESULT to Analytics.Value.OK))
                handleBarcodeResult(barcode)
            }
            .addOnFailureListener { e ->
                if (e is MlKitException) {
                    val errorCode = e.errorCode
                    Analytics.log(TAG, "MlKitException errorCode: $errorCode")
                    when (errorCode) {
                        MlKitException.CODE_SCANNER_UNAVAILABLE -> {}
                        MlKitException.CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD -> {}
                        else -> {
                            try {
                                val gpsPackageInfo: PackageInfo = this@SearchActivity.packageManager.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0)
                                Analytics.log(TAG, "Google Play Services versionName: ${gpsPackageInfo.versionName}")
                                Analytics.log(TAG, "Google Play Services versionCode: ${PackageInfoCompat.getLongVersionCode(gpsPackageInfo)}")
                            } catch (ex: PackageManager.NameNotFoundException) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
                this.onScannerFailure(e)
            }
    }

    private fun onScannerFailure(e: Exception) {
        Analytics.logEvent(Analytics.Event.SCAN, bundleOf(Analytics.Param.RESULT to e.message))
        this.showAlert(e)
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
            Log.d(TAG, "[scanner]: got $it")
            searchTextView?.setText(it)
            setSearchClass(SearchClass.IDENTIFIER)
            fetchSearchResults()
        }
    }

    private fun setSearchClass(value: String) {
        // Set the searchClassSpinner to the specified value. Because we are changing the state
        // of the spinner, force the searchOptionsButton on to ensure the spinner is visible.
        searchClassOption.selectByValue(value)
        searchOptionsButton?.isChecked = true
    }

    companion object {
        private val TAG = SearchActivity::class.java.simpleName

        const val RESULT_CODE_NORMAL = 10
        const val RESULT_CODE_SEARCH_BY_AUTHOR = 11
        const val RESULT_CODE_SEARCH_BY_KEYWORD = 12
    }
}
