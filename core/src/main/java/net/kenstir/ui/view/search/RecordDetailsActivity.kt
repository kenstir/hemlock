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
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import net.kenstir.hemlock.R
import net.kenstir.ui.Key
import net.kenstir.data.model.BibRecord
import net.kenstir.logging.Log
import net.kenstir.ui.App
import org.evergreen_ils.system.EgOrg
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.logBundleSize
import net.kenstir.ui.view.details.DetailsFragment
import net.kenstir.ui.view.search.SearchActivity.Companion.RESULT_CODE_NORMAL

class RecordDetailsActivity : BaseActivity() {
    private var mPager: ViewPager? = null
    private val records = ArrayList<BibRecord>()
    private var orgID = 1
    private var numResults = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_record_details)
        setupActionBar(intent.getStringExtra(Key.TITLE))
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        // Copy either serialized recordList or search results into our own ArrayList.
        // This is an attempt to fix an IllegalStateException crash (see commit for details).
        var recordList = intent.getSerializableExtra(Key.RECORD_LIST) as? List<BibRecord>
        if (recordList == null)
            recordList = App.getServiceConfig().searchService.getLastSearchResults().records
        records.clear()
        records.addAll(recordList)

        // Calculate numResults after records are loaded
        orgID = intent.getIntExtra(Key.ORG_ID, EgOrg.consortiumID)
        val recordPosition = intent.getIntExtra(Key.RECORD_POSITION, 0)
        numResults = intent.getIntExtra(Key.NUM_RESULTS, records.size)
        mPager = findViewById(R.id.main_content_view)
        mPager?.adapter = SearchFragmentAdapter(supportFragmentManager)
        mPager?.currentItem = recordPosition

        // Fix TransactionTooLargeException attempt 1: disable saving ViewPager state
        // This works, but it has the side effect of losing the current page on rotation.
//        val limit = mPager?.offscreenPageLimit ?: 0
//        Log.d("RecordDetails", "offscreenPageLimit=$limit")
//        mPager?.isSaveEnabled = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.logBundleSize(outState)

        // Fix TransactionTooLargeException attempt 2: delete viewHierarchy state
        // This also works, but has the same side effect as above.
//        outState.remove("android:viewHierarchyState")
//        this.logBundleSize(outState, "RecordDetailsActivity")
    }

    private fun finishWithIntent() {
        val intent = Intent()
        setResult(RESULT_CODE_NORMAL, intent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishWithIntent()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finishWithIntent()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class SearchFragmentAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(
        fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        override fun getItem(position: Int): Fragment {
            return DetailsFragment.create(records[position], orgID, position, numResults)
        }

        override fun getCount(): Int {
            return records.size
        }
    }
}
