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
package net.kenstir.ui.view.bookbags

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.PatronList
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.Key
import net.kenstir.ui.util.ItemClickSupport
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.search.DividerItemDecoration
import net.kenstir.util.Analytics

class BookBagsActivity : BaseActivity(), BookBagCreateDialogFragment.CreateListener {
    private var rv: RecyclerView? = null
    private var adapter: BookBagViewAdapter? = null
    private var items = mutableListOf<PatronList>()
    private var progress: ProgressDialogSupport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_bookbags)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        // prevent soft keyboard from popping up when the activity starts
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        progress = ProgressDialogSupport()

        rv = findViewById(R.id.recycler_view)
        adapter = BookBagViewAdapter(items)
        rv?.adapter = adapter
        rv?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST))
        ItemClickSupport.addTo(rv ?: return).setOnItemClickListener { _, position, _ ->
            val intent = Intent(this@BookBagsActivity, BookBagDetailsActivity::class.java)
            intent.putExtra(Key.PATRON_LIST, items[position])
            startActivityForResult(intent, 0)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookbags, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_create_bookbag -> {
                showCreateBookBagDialog()
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@BookBagsActivity, getString(R.string.msg_retrieving_lists))

                // load bookbags
                val result = App.getServiceConfig().userService.loadPatronLists(
                    App.getAccount())
                when (result) {
                    is Result.Success -> {}
                    is Result.Error -> { showAlert(result.exception); return@async }
                }
                val patronLists = App.getAccount().patronLists

                // load bookbag items
                val jobs = mutableListOf<Deferred<Any>>()
                for (list in patronLists) {
                    jobs.add(scope.async {
                        App.getServiceConfig().userService.loadPatronListItems(App.getAccount(), list)
                    })
                }
                jobs.map { it.await() }

                updateList(patronLists)
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updateList(patronLists: List<PatronList>) {
        items.clear()
        items.addAll(patronLists)
        adapter?.notifyDataSetChanged()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_CODE_UPDATE -> fetchData()
        }
    }

    private fun showCreateBookBagDialog() {
        val dialogFragment = BookBagCreateDialogFragment()
        dialogFragment.show(supportFragmentManager, BookBagCreateDialogFragment.TAG)
    }

    private fun createBookBag(name: String, description: String) {
        scope.async {
            progress?.show(this@BookBagsActivity, getString(R.string.msg_creating_list))
            val result = App.getServiceConfig().userService.createPatronList(
                App.getAccount(), name, description)
            progress?.dismiss()
            Analytics.logEvent(Analytics.Event.BOOKBAGS_CREATE_LIST, bundleOf(
                Analytics.Param.RESULT to Analytics.resultValue(result)
            ))
            when (result) {
                is Result.Error -> showAlert(result.exception)
                is Result.Success -> fetchData()
            }
        }
    }

    override fun onBookBagCreated(name: String, description: String) {
        createBookBag(name, description)
    }

    companion object {
        private val TAG = BookBagsActivity::class.java.simpleName
    }
}
