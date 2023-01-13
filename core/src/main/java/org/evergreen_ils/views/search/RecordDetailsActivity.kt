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
package org.evergreen_ils.views.search

import org.evergreen_ils.utils.ui.BaseActivity
import androidx.viewpager.widget.ViewPager
import android.os.Bundle
import org.evergreen_ils.android.App
import org.evergreen_ils.R
import org.evergreen_ils.utils.ui.ActionBarUtils
import org.evergreen_ils.system.EgSearch
import org.evergreen_ils.system.EgOrg
import android.content.Intent
import android.view.KeyEvent
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.utils.ui.DetailsFragment
import java.util.ArrayList

class RecordDetailsActivity : BaseActivity() {
    private var mPager: ViewPager? = null
    private val records = ArrayList<MBRecord>()
    private var orgID = 1
    private var numResults = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.record_details)
        ActionBarUtils.initActionBarForActivity(this, intent.getStringExtra("title"))

        // Copy either serialized recordList or search results into our own ArrayList.
        // This is an attempt to fix an IllegalStateException crash (see commit for details).
        var recordList = intent.getSerializableExtra("recordList") as? ArrayList<MBRecord>
        if (recordList == null) recordList = EgSearch.results
        records.clear()
        records.addAll(recordList)

        // Calculate numResults after records are loaded
        orgID = intent.getIntExtra("orgID", EgOrg.consortiumID)
        val recordPosition = intent.getIntExtra("recordPosition", 0)
        numResults = intent.getIntExtra("numResults", records.size)
        mPager = findViewById(R.id.pager)
        mPager?.adapter = SearchFragmentAdapter(supportFragmentManager)
        mPager?.currentItem = recordPosition
    }

    private fun finishWithIntent() {
        val intent = Intent()
        setResult(RETURN_DATA, intent)
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
            //onBackPressed();
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

    companion object {
        private val TAG = RecordDetailsActivity::class.java.simpleName
        const val RETURN_DATA = 5
    }
}