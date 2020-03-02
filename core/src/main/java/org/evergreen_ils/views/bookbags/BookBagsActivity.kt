/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.StringUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert

class BookBagsActivity : BaseActivity() {
    private var lv: ListView? = null
    private var listAdapter: BookBagsArrayAdapter? = null
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
            createBookbag()
        })
        lv = findViewById(R.id.bookbag_list)
        listAdapter = BookBagsArrayAdapter(this, R.layout.bookbag_list_item)
        lv?.setAdapter(listAdapter)
        lv?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            Analytics.logEvent("Lists: Tap List")
            val item = lv?.getItemAtPosition(position) as BookBag
            val intent = Intent(this@BookBagsActivity, BookBagDetailsActivity::class.java)
            intent.putExtra("bookBag", item)
            startActivityForResult(intent, 0)
        })
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

    private fun fetchData() {
        async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@BookBagsActivity, getString(R.string.msg_retrieving_lists))
                bookBagName?.text = null

                // fetch bookbags
                val result = Gateway.actor.fetchBookBags(App.getAccount())
                when (result) {
                    is Result.Success -> {
                        App.getAccount().bookBags = BookBag.makeArray(result.data)
                    }
                    is Result.Error -> {
                        showAlert(result.exception)
                        return@async
                    }
                }

                // flesh bookbags
                for (bookBag in App.getAccount().bookBags) {
                    jobs.add(async {
                        fetchBookBagDetails(bookBag, App.getAccount())
                    })
                }

                jobs.joinAll()
                updateList()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private suspend fun fetchBookBagDetails(bookBag: BookBag, account: Account): Result<Unit> {
        Log.d(TAG, "[kcxxx] bag:${bookBag.name}")
        val result = Gateway.actor.fleshBookBagAsync(account, bookBag.id)
        if (result is Result.Error) return result
        val obj = result.get()
        Log.d(TAG, "[kcxxx] bag content:$obj")
        bookBag.fleshFromObject(obj)

        return Result.Success(Unit)
    }

    private fun updateList() {
        listAdapter?.clear()
        listAdapter?.addAll(App.getAccount().bookBags)
        listAdapter?.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            BookBagDetailsActivity.RESULT_CODE_UPDATE -> fetchData()
        }
    }

    private fun createBookbag() {
        val name = bookBagName?.text.toString().trim()
        if (name.isEmpty()) {
            bookBagName?.error = getString(R.string.error_list_name_empty)
            return
        }
        async {
            progress?.show(this@BookBagsActivity, getString(R.string.msg_creating_list))
            Analytics.logEvent("Lists: Create List")
            val result = Gateway.actor.createBookBagAsync(App.getAccount(), name)
            progress?.dismiss()
            when (result) {
                is Result.Error -> showAlert(result.exception)
                is Result.Success -> fetchData()
            }
        }
    }

    internal inner class BookBagsArrayAdapter(context: Context, private val resourceId: Int) : ArrayAdapter<BookBag>(context, resourceId) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var row = when(convertView) {
                null -> {
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    inflater.inflate(resourceId, parent, false)
                }
                else -> {
                    convertView
                }
            }

            val nameText = row.findViewById<View>(R.id.bookbag_name) as TextView
            val descText = row.findViewById<View>(R.id.bookbag_description) as TextView
            val itemsText = row.findViewById<View>(R.id.bookbag_items) as TextView

            val record = getItem(position)
            nameText.text = StringUtils.safeString(record.name)
            descText.text = StringUtils.safeString(record.description)
            itemsText.text = resources.getQuantityString(R.plurals.number_of_items,
                    record.items!!.size, record.items!!.size)

            return row
        }

    }

    companion object {
        private val TAG = BookBagsActivity::class.java.simpleName
    }
}
