/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
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

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.os.bundleOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.BookBagItem
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.views.search.RecordDetails
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.utils.pubdateSortKey
import org.evergreen_ils.utils.titleSortKey
import org.evergreen_ils.utils.ui.*
import java.util.*

const val RESULT_CODE_UPDATE = 1
const val SORT_BY_STATE_KEY = "sort_by"

class BookBagDetailsActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    private var lv: ListView? = null
    private var listAdapter: BookBagItemsArrayAdapter? = null
    private var progress: ProgressDialogSupport? = null
    private lateinit var bookBag: BookBag
    private var sortedItems = ArrayList<BookBagItem>()
    private var bookBagName: TextView? = null
    private var bookBagDescription: TextView? = null

    private lateinit var sortByKeywords: Array<String>
    private lateinit var SORT_BY_AUTHOR: String
    private lateinit var SORT_BY_PUBDATE: String
    private lateinit var SORT_BY_TITLE: String
    private var sortBySelectedIndex = 0

    private val sortByKeyword: String
        get() {
            return sortByKeywords[sortBySelectedIndex]
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.bookbagitem_list)
        ActionBarUtils.initActionBarForActivity(this)

        progress = ProgressDialogSupport()
        bookBag = intent.getSerializableExtra("bookBag") as BookBag

        bookBagName = findViewById(R.id.bookbag_name)
        bookBagDescription = findViewById(R.id.bookbag_description)

        bookBagName?.text = bookBag.name
        bookBagDescription?.text = bookBag.description

        lv = findViewById(R.id.bookbagitem_list)
        listAdapter = BookBagItemsArrayAdapter(this, R.layout.bookbagitem_list_item, sortedItems)
        lv?.adapter = listAdapter
        lv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            //Analytics.logEvent("list_itemclick")
            val records = ArrayList<MBRecord?>()
            sortedItems.let {
                for (item in it) {
                    records.add(item.record)
                }
            }
            RecordDetails.launchDetailsFlow(this@BookBagDetailsActivity, records, position)
        }

        initSortBy()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_bookbag_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_bookbag_delete) {
            confirmDeleteList()
            return true
        } else if (id == R.id.action_bookbag_sort) {
            showSortListDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initSortBy() {
        sortByKeywords = resources.getStringArray(R.array.sort_by_keyword)
        SORT_BY_AUTHOR = resources.getString(R.string.sort_by_author_keyword)
        SORT_BY_PUBDATE = resources.getString(R.string.sort_by_pubdate_keyword)
        SORT_BY_TITLE = resources.getString(R.string.sort_by_title_keyword)

        // the default sort is last used, pubdate by default
        val keyword = AppState.getString(SORT_BY_STATE_KEY, SORT_BY_PUBDATE)
        val index = if (keyword in sortByKeywords) sortByKeywords.indexOf(keyword) else sortByKeywords.indexOf(SORT_BY_PUBDATE)
        sortBySelectedIndex = index
    }

    private fun fetchData() {
        async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@BookBagDetailsActivity, getString(R.string.msg_retrieving_list_contents))

                // fetch bookBag contents
                val result = GatewayLoader.loadBookBagContents(App.getAccount(), bookBag, resources.getBoolean(R.bool.ou_extra_bookbag_query))
                if (result is Result.Error) { showAlert(result.exception); return@async }

                // fetch item details
                bookBag.items.let {
                    for (item in it) {
                        jobs.add(async {
                            fetchTargetDetails(item)
                        })
                    }
                }

                jobs.joinAll()
                updateItemsList()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    suspend fun fetchTargetDetails(item: BookBagItem): Result<Unit> {
        val modsResult = Gateway.search.fetchRecordMODS(item.targetId)
        if (modsResult is Result.Error) return modsResult
        val modsObj = modsResult.get()
        item.record = MBRecord(modsObj)

        return Result.Success(Unit)
    }

    private fun updateItemsList() {
        val comparator = when (sortByKeyword) {
            SORT_BY_AUTHOR -> BookBagItemAuthorComparator(false)
            SORT_BY_PUBDATE -> BookBagItemPubdateComparator(true)
            SORT_BY_TITLE -> BookBagItemTitleComparator(false)
            else -> BookBagItemPubdateComparator(true)
        }

        sortedItems.clear()
        sortedItems.addAll(bookBag.items.sortedWith(comparator))
        listAdapter?.notifyDataSetChanged()
    }

    private fun confirmDeleteList() {
        val builder = AlertDialog.Builder(this@BookBagDetailsActivity)
        builder.setMessage(R.string.delete_list_confirm_msg)
        builder.setNegativeButton(R.string.delete_list_negative_button, null)
        builder.setPositiveButton(R.string.delete_list_positive_button) { dialog, which -> deleteList() }
        builder.create().show()
    }

    private fun deleteList() {
        async {
            progress?.show(this@BookBagDetailsActivity, getString(R.string.msg_deleting_list))
            val id = bookBag.id
            val result = if (id != null) {
                Gateway.actor.deleteBookBagAsync(App.getAccount(), id)
            } else {
                Result.Success(Unit)
            }
            progress?.dismiss()
            Analytics.logEvent(Analytics.Event.BOOKBAGS_DELETE_LIST, bundleOf(
                Analytics.Param.RESULT to Analytics.resultValue(result)
            ))
            when (result) {
                is Result.Error -> showAlert(result.exception)
                is Result.Success -> {
                    setResult(RESULT_CODE_UPDATE)
                    finish()
                }
            }
        }
    }

    private fun showSortListDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.sort_by_message)
        builder.setSingleChoiceItems(R.array.sort_by, sortBySelectedIndex) { dialog, which ->
            this.sortBySelectedIndex = which
            AppState.setString(SORT_BY_STATE_KEY, sortByKeyword)
            updateItemsList()
            dialog.dismiss()
        }
        builder.create().show()
    }

    internal class BookBagItemAuthorComparator(descending: Boolean): Comparator<BookBagItem> {
        private val descending = descending

        override fun compare(o1: BookBagItem?, o2: BookBagItem?): Int {
            val key1 = if (descending) o2?.record?.author else o1?.record?.author
            val key2 = if (descending) o1?.record?.author else o2?.record?.author
            return when {
                key1 == null && key2 == null -> 0
                key1 == null -> -1
                key2 == null -> 1
                else -> key1.compareTo(key2)
            }
        }
    }

    internal class BookBagItemPubdateComparator(descending: Boolean): Comparator<BookBagItem> {
        private val descending = descending

        override fun compare(o1: BookBagItem?, o2: BookBagItem?): Int {
            val key1 = if (descending) pubdateSortKey(o2?.record?.pubdate) else pubdateSortKey(o1?.record?.pubdate)
            val key2 = if (descending) pubdateSortKey(o1?.record?.pubdate) else pubdateSortKey(o2?.record?.pubdate)
            return when {
                key1 == null && key2 == null -> 0
                key1 == null -> -1
                key2 == null -> 1
                else -> key1.compareTo(key2)
            }
        }
    }

    internal class BookBagItemTitleComparator(descending: Boolean): Comparator<BookBagItem> {
        private val descending = descending

        override fun compare(o1: BookBagItem?, o2: BookBagItem?): Int {
            val key1 = if (descending) titleSortKey(o2?.record?.title) else titleSortKey(o1?.record?.title)
            val key2 = if (descending) titleSortKey(o1?.record?.title) else titleSortKey(o2?.record?.title)
            return when {
                key1 == null && key2 == null -> 0
                key1 == null -> -1
                key2 == null -> 1
                else -> key1.compareTo(key2)
            }
        }
    }

    internal inner class BookBagItemsArrayAdapter(
        context: Context,
        private val resourceId: Int,
        items: ArrayList<BookBagItem>
    ) : ArrayAdapter<BookBagItem>(context, resourceId, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = when(convertView) {
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
            title.text = record?.record?.title
            author.text = record?.record?.author
            remove.setOnClickListener(View.OnClickListener {
                val id = record?.id ?: return@OnClickListener
                async {
                    progress?.show(this@BookBagDetailsActivity, getString(R.string.msg_removing_list_item))
                    val result = Gateway.actor.removeItemFromBookBagAsync(App.getAccount(), id)
                    progress?.dismiss()
                    Analytics.logEvent(Analytics.Event.BOOKBAG_DELETE_ITEM, bundleOf(
                        Analytics.Param.RESULT to Analytics.resultValue(result)
                    ))
                    when (result) {
                        is Result.Error -> showAlert(result.exception)
                        is Result.Success -> {
                            setResult(RESULT_CODE_UPDATE)
                            fetchData()
                        }
                    }
                }
            })

            return row
        }
    }
}
