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

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.accountAccess.SessionNotFoundException
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.BookBagItem
import org.evergreen_ils.searchCatalog.RecordDetails
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.utils.ui.ActionBarUtils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import java.util.*

class BookBagDetailsActivity : BaseActivity() {
    private var accountAccess: AccountAccess? = null
    private var lv: ListView? = null
    private var listAdapter: BookBagItemsArrayAdapter? = null
    private var progress: ProgressDialogSupport? = null
    private var bookBag: BookBag? = null
    private var bookBagName: TextView? = null
    private var bookBagDescription: TextView? = null
    private var deleteButton: Button? = null
    private var getItemsRunnable: Runnable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.bookbagitem_list)

        ActionBarUtils.initActionBarForActivity(this)

        accountAccess = AccountAccess.getInstance()
        progress = ProgressDialogSupport()
        bookBag = intent.getSerializableExtra("bookBag") as BookBag

        bookBagName = findViewById(R.id.bookbag_name)
        bookBagDescription = findViewById(R.id.bookbag_description)
        deleteButton = findViewById(R.id.remove_bookbag)

        bookBagName?.setText(bookBag?.name)
        bookBagDescription?.setText(bookBag?.description)
        deleteButton?.setOnClickListener(View.OnClickListener {
            Analytics.logEvent("Lists: Delete List")
            val builder = AlertDialog.Builder(this@BookBagDetailsActivity)
            builder.setMessage(R.string.delete_list_confirm_msg)
            builder.setNegativeButton(R.string.delete_list_negative_button, null)
            builder.setPositiveButton(R.string.delete_list_positive_button) { dialog, which -> deleteList() }
            builder.create().show()
        })
        lv = findViewById(R.id.bookbagitem_list)
        listAdapter = BookBagItemsArrayAdapter(this, R.layout.bookbagitem_list_item)
        lv?.setAdapter(listAdapter)
        lv?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            Analytics.logEvent("Lists: Tap List Item")
            val records = ArrayList<RecordInfo?>()
            bookBag?.items?.let {
                for (item in it) {
                    records.add(item.recordInfo)
                }
            }
            RecordDetails.launchDetailsFlow(this@BookBagDetailsActivity, records, position)
        })
        initRunnable()
        Thread(getItemsRunnable).start()
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    private fun initRunnable() {
        getItemsRunnable = Runnable {
            runOnUiThread { progress!!.show(this@BookBagDetailsActivity, getString(R.string.msg_retrieving_list_contents)) }
            val ids = ArrayList<Int>()
            for (i in bookBag!!.items.indices) {
                ids.add(bookBag!!.items[i].targetId)
            }
            val records = AccountAccess.getInstance().getRecordsInfo(ids)
            for (i in bookBag!!.items.indices) {
                bookBag!!.items[i].recordInfo = records[i]
            }
            runOnUiThread {
                progress!!.dismiss()
                if (bookBag!!.items.isEmpty()) Toast.makeText(this@BookBagDetailsActivity, R.string.msg_list_empty, Toast.LENGTH_LONG).show()
                updateListAdapter()
            }
        }
    }

    private fun updateListAdapter() {
        listAdapter?.clear()
        bookBag?.items.let { listAdapter?.addAll(it) }
        listAdapter?.notifyDataSetChanged()
    }

    private fun deleteList() {
        progress?.show(this, getString(R.string.msg_deleting_list))
        val thread = Thread(Runnable {
            try {
                accountAccess!!.deleteBookBag(bookBag!!.id)
            } catch (e: SessionNotFoundException) {
                Log.d(TAG, "caught", e)
            }
            runOnUiThread {
                progress?.dismiss()
                setResult(RESULT_CODE_UPDATE)
                finish()
            }
        })
        thread.start()
    }

    internal inner class BookBagItemsArrayAdapter(context: Context, private val resourceId: Int) : ArrayAdapter<BookBagItem>(context, resourceId) {

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

            val title = row.findViewById<View>(R.id.bookbagitem_title) as TextView
            val author = row.findViewById<View>(R.id.bookbagitem_author) as TextView
            val remove = row.findViewById<View>(R.id.bookbagitem_remove_button) as Button

            val record = getItem(position)
            title.setText(record!!.recordInfo!!.title)
            author.setText(record.recordInfo!!.author)
            remove.setOnClickListener(View.OnClickListener {
                Analytics.logEvent("Lists: Remove List Item")
                val removeItem = Thread(Runnable {
                    runOnUiThread { progress!!.show(this@BookBagDetailsActivity, getString(R.string.msg_removing_list_item)) }
                    try {
                        accountAccess!!.removeBookbagItem(record.id)
                    } catch (e: SessionNotFoundException) {
                        try {
                            if (accountAccess!!.reauthenticate(this@BookBagDetailsActivity)) accountAccess!!.removeBookbagItem(record.id)
                        } catch (e1: Exception) {
                            Log.d(TAG, "caught", e1)
                        }
                    }
                    runOnUiThread {
                        progress!!.dismiss()
                        setResult(RESULT_CODE_UPDATE)
                        bookBag!!.items.remove(record)
                        Thread(getItemsRunnable).start()
                    }
                })
                removeItem.start()
            })

            return row
        }
    }

    companion object {
        private val TAG = BookBagDetailsActivity::class.java.simpleName
        const val RESULT_CODE_UPDATE = 1
    }
}
