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
package org.evergreen_ils.views.bookbags

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.os.bundleOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Key
import net.kenstir.hemlock.logging.Log
import org.evergreen_ils.data.BookBag
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.ui.BaseActivity
import net.kenstir.hemlock.android.ui.ProgressDialogSupport
import net.kenstir.hemlock.android.ui.showAlert
import net.kenstir.hemlock.data.model.PatronList

class BookBagsActivity : BaseActivity() {
    private var lv: ListView? = null
    private var listAdapter: PatronListArrayAdapter? = null
    private var progress: ProgressDialogSupport? = null
    private var bookBagName: EditText? = null
    private var createButton: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_bookbags)

        // prevent soft keyboard from popping up when the activity starts
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        progress = ProgressDialogSupport()
        bookBagName = findViewById(R.id.bookbag_create_name)
        createButton = findViewById(R.id.bookbag_create_button)
        createButton?.setOnClickListener(View.OnClickListener {
            createBookBag()
        })
        lv = findViewById(R.id.bookbag_list)
        listAdapter = PatronListArrayAdapter(this, R.layout.bookbag_list_item)
        lv?.adapter = listAdapter
        lv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val item = lv?.getItemAtPosition(position) as BookBag
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

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@BookBagsActivity, getString(R.string.msg_retrieving_lists))
                bookBagName?.text = null

                // load bookbags
                val result = App.getServiceConfig().userService.loadPatronLists(App.getAccount())
                when (result) {
                    is Result.Success -> {}
                    is Result.Error -> { showAlert(result.exception); return@async }
                }

                // load bookbag items
                val jobs = mutableListOf<Deferred<Any>>()
                for (list in App.getAccount().patronLists) {
                    jobs.add(scope.async {
                        App.getServiceConfig().userService.loadPatronListItems(App.getAccount(), list, resources.getBoolean(R.bool.ou_extra_bookbag_query))
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

    private fun createBookBag() {
        val name = bookBagName?.text.toString().trim()
        if (name.isEmpty()) {
            bookBagName?.error = getString(R.string.error_list_name_empty)
            return
        }
        scope.async {
            progress?.show(this@BookBagsActivity, getString(R.string.msg_creating_list))
            val result = App.getServiceConfig().userService.createPatronList(App.getAccount(), name)
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
