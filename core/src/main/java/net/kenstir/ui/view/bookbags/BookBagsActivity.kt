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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.core.os.bundleOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.PatronList
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.util.Analytics
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.Key
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert

class BookBagsActivity : BaseActivity(), BookBagCreateDialogFragment.CreateListener {
    private var lv: ListView? = null
    private var listAdapter: PatronListArrayAdapter? = null
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

        lv = findViewById(R.id.list_view)
        listAdapter = PatronListArrayAdapter(this, R.layout.bookbag_list_item)
        lv?.adapter = listAdapter
        lv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val item = lv?.getItemAtPosition(position) as PatronList
            val intent = Intent(this@BookBagsActivity, BookBagDetailsActivity::class.java)
            intent.putExtra(Key.PATRON_LIST, item)
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

                // load bookbag items
                val jobs = mutableListOf<Deferred<Any>>()
                for (list in App.getAccount().patronLists) {
                    jobs.add(scope.async {
                        App.getServiceConfig().userService.loadPatronListItems(
                            App.getAccount(), list, resources.getBoolean(R.bool.ou_extra_bookbag_query))
                    })
                }
                jobs.map { it.await() }

                updateListAdapter()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updateListAdapter() {
        listAdapter?.clear()
        listAdapter?.addAll(App.getAccount().patronLists)
        listAdapter?.notifyDataSetChanged()
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

    internal inner class PatronListArrayAdapter(context: Context, private val resourceId: Int) : ArrayAdapter<PatronList>(context, resourceId) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row = when(convertView) {
                null -> {
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    inflater.inflate(resourceId, parent, false)
                }
                else -> {
                    convertView
                }
            }

            val nameText = row.findViewById<TextView>(R.id.bookbag_name)
            val descText = row.findViewById<TextView>(R.id.bookbag_description)
            val itemsText = row.findViewById<TextView>(R.id.bookbag_items)

            val record = getItem(position)
            nameText.text = record?.name
            descText.text = record?.description
            itemsText.text = resources.getQuantityString(R.plurals.number_of_items,
                    record?.items?.size ?: 0, record?.items?.size ?: 0)

            return row
        }

    }

    companion object {
        private val TAG = BookBagsActivity::class.java.simpleName
    }
}
