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
package org.evergreen_ils.accountAccess.bookbags

import android.app.AlertDialog
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
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.accountAccess.SessionNotFoundException
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.utils.StringUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import java.util.*

class BookBagActivity : BaseActivity() {
    private var accountAccess: AccountAccess? = null
    private var lv: ListView? = null
    private var listAdapter: BookBagsArrayAdapter? = null
    private var bookBags: ArrayList<BookBag>? = null
    private var progress: ProgressDialogSupport? = null
    private var bookbag_name: EditText? = null
    private var create_bookbag: Button? = null
    private var getBookbagsRunnable: Runnable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_bookbags)

        // prevent soft keyboard from popping up when the activity starts
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        accountAccess = AccountAccess.getInstance()
        progress = ProgressDialogSupport()
        bookbag_name = findViewById(R.id.bookbag_create_name)
        create_bookbag = findViewById(R.id.bookbag_create_button)
        create_bookbag?.setOnClickListener(View.OnClickListener {
            createBookbag(bookbag_name?.getText().toString())
        })
        lv = findViewById(R.id.bookbag_list)
        bookBags = ArrayList()
        listAdapter = BookBagsArrayAdapter(this, R.layout.bookbag_list_item, bookBags!!)
        lv?.setAdapter(listAdapter)
        lv?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            Analytics.logEvent("Lists: Tap List")
            val item = lv?.getItemAtPosition(position) as BookBag
            val intent = Intent(this@BookBagActivity, BookBagDetailsActivity::class.java)
            intent.putExtra("bookBag", item)
            startActivityForResult(intent, 0)
        })
        initGetBookbagsRunnable()
        Thread(getBookbagsRunnable).start()
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
                progress?.show(this@BookBagActivity, getString(R.string.msg_retrieving_lists))

                // fetch bookbags
                val result = Gateway.actor.fetchUserBookBags(App.getAccount())
                when (result) {
                    is Result.Success -> {
                        bookBags = BookBag.makeArray(result.data)
                    }
                    is Result.Error -> {
                        showAlert(result.exception)
                        return@async
                    }
                }

                jobs.joinAll()
                //updateList()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun initGetBookbagsRunnable() {
        /*
        getBookbagsRunnable = Runnable {
            runOnUiThread { if (!isFinishing) progress!!.show(this@BookBagActivity, getString(R.string.msg_retrieving_lists)) }
            try {
                accountAccess!!.retrieveBookbags()
            } catch (e: SessionNotFoundException) {
                try {
                    if (accountAccess!!.reauthenticate(this@BookBagActivity)) accountAccess!!.retrieveBookbags()
                } catch (e2: Exception) {
                    Log.d(TAG, "caught", e2)
                }
            }
            bookBags = accountAccess!!.bookbags
            runOnUiThread {
                listAdapter!!.clear()
                for (bookBag in bookBags) listAdapter!!.add(bookBag)
                listAdapter!!.notifyDataSetChanged()
                progress!!.dismiss()
            }
        }
        */
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            BookBagDetailsActivity.RESULT_CODE_UPDATE -> Thread(getBookbagsRunnable).start()
        }
    }

    private fun createBookbag(name: String) {
        if (name.length < 2) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.msg_list_name_too_short_title)
                    .setMessage(R.string.msg_list_name_too_short)
                    .setPositiveButton(android.R.string.ok, null)
            builder.create().show()
            return
        }
        Analytics.logEvent("Lists: Create List")
        val thread = Thread(Runnable {
            try {
                accountAccess!!.createBookbag(name)
            } catch (e: SessionNotFoundException) {
                try {
                    if (accountAccess!!.reauthenticate(this@BookBagActivity)) accountAccess!!.createBookbag(name)
                } catch (eauth: Exception) {
                    Log.d(TAG, "caught", eauth)
                }
            }
            runOnUiThread { progress!!.dismiss() }
            Thread(getBookbagsRunnable).start()
        })
        progress!!.show(this, getString(R.string.msg_creating_list))
        thread.start()
    }

    internal inner class BookBagsArrayAdapter(context: Context, private val resourceId: Int, private val items: List<BookBag>) : ArrayAdapter<BookBag>(context, resourceId, items) {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(index: Int): BookBag {
            return items[index]
        }

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
        private val TAG = BookBagActivity::class.java.simpleName
    }
}