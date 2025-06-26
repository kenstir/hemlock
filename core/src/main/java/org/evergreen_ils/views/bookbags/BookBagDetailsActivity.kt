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

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.os.bundleOf
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.AppState
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.android.ui.ActionBarUtils
import net.kenstir.hemlock.android.ui.ProgressDialogSupport
import net.kenstir.hemlock.android.ui.showAlert
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.BookBagItem
import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.model.ListItem
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.data.MBRecord
import net.kenstir.hemlock.util.pubdateSortKey
import org.evergreen_ils.utils.ui.*
import java.text.Collator
import java.util.*

const val RESULT_CODE_UPDATE = 1
const val SORT_BY_STATE_KEY = "sort_by"
const val SORT_DESC_STATE_KEY = "sort_desc"

class BookBagDetailsActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    private var lv: ListView? = null
    private var listAdapter: ListItemArrayAdapter? = null
    private var progress: ProgressDialogSupport? = null
    private lateinit var bookBag: BookBag
    private var sortedItems = ArrayList<ListItem>()
    private var bookBagName: TextView? = null
    private var bookBagDescription: TextView? = null

    private lateinit var sortByKeywords: Array<String>
    private lateinit var SORT_BY_AUTHOR: String
    private lateinit var SORT_BY_PUBDATE: String
    private lateinit var SORT_BY_TITLE: String
    private var sortBySelectedIndex = 0
    private var sortDescending = true

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
        listAdapter = ListItemArrayAdapter(this, R.layout.bookbagitem_list_item, sortedItems)
        lv?.adapter = listAdapter
        lv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            //Analytics.logEvent("list_itemclick")
            TODO("fixme")
//            val records = ArrayList<MBRecord?>()
//            sortedItems.let {
//                for (item in it) {
//                    records.add(item.record)
//                }
//            }
//            RecordDetails.launchDetailsFlow(this@BookBagDetailsActivity, records, position)
        }

        initSortBy()
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
        menuInflater.inflate(R.menu.menu_bookbag_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // According to https://developer.android.com/develop/ui/views/components/menus#ChangingTheMenu
    // one should make runtime menu changes in onPrepareOptionsMenu, not onCreateOptionsMenu.
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // whichever way the sort is going, remove the other menuItem
        menu?.removeItem(if (sortDescending) { R.id.action_bookbag_sort_asc } else R.id.action_bookbag_sort_desc)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bookbag_delete -> {
                confirmDeleteList()
                return true
            }
            R.id.action_bookbag_sort -> {
                showSortListDialog()
                return true
            }
            R.id.action_bookbag_sort_asc, R.id.action_bookbag_sort_desc -> {
                reverseSortOrder()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initSortBy() {
        sortByKeywords = resources.getStringArray(R.array.sort_by_keyword)
        SORT_BY_AUTHOR = resources.getString(R.string.sort_by_author_keyword)
        SORT_BY_PUBDATE = resources.getString(R.string.sort_by_pubdate_keyword)
        SORT_BY_TITLE = resources.getString(R.string.sort_by_title_keyword)

        // the default sort is whatever was last selected; pubdate descending by default
        sortDescending = AppState.getBoolean(SORT_DESC_STATE_KEY, true)
        val keyword = AppState.getString(SORT_BY_STATE_KEY, SORT_BY_PUBDATE)
        val index = if (keyword in sortByKeywords) sortByKeywords.indexOf(keyword) else sortByKeywords.indexOf(SORT_BY_PUBDATE)
        sortBySelectedIndex = index
    }

    private fun fetchData() {
        scope.async {
            try {
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                progress?.show(this@BookBagDetailsActivity, getString(R.string.msg_retrieving_list_contents))

                // load bookBag contents
                val result = App.getServiceConfig().userService.loadPatronListItems(App.getAccount(), bookBag.id)
                if (result is Result.Error) { showAlert(result.exception); return@async }

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
        val record = MBRecord(modsObj)
        item.record = record

        if (resources.getBoolean(R.bool.ou_need_marc_record)) {
            val result = Gateway.pcrud.fetchMARC(record.id)
            if (result is Result.Error) return result
            val breObj = result.get()
            record.updateFromBREResponse(breObj)
        }

        return Result.Success(Unit)
    }

    private fun updateItemsList() {
        val comparator = when (sortByKeyword) {
            SORT_BY_AUTHOR -> ListItemAuthorComparator(sortDescending)
            SORT_BY_PUBDATE -> ListItemPubdateComparator(sortDescending)
            SORT_BY_TITLE -> ListItemTitleComparator(sortDescending)
            else -> ListItemPubdateComparator(sortDescending)
        }

        sortedItems.clear()
        sortedItems.addAll(bookBag.items.sortedWith(comparator))
        listAdapter?.notifyDataSetChanged()

//        val direction = if (sortDescending) { "desc" } else "asc"
//        Log.d(TAG, "[sort] $sortByKeyword $direction")
//        for (item in sortedItems) {
//            item.record?.let {
//                Log.d(TAG, "[sort] key <<${it.titleSort.take(32).padEnd(32)}>> title <<${it.title.take(32).padEnd(32)}>> nf ${it.nonFilingCharacters} id ${it.id}")
//            }
//        }
    }

    private fun confirmDeleteList() {
        val builder = AlertDialog.Builder(this@BookBagDetailsActivity)
        builder.setMessage(R.string.delete_list_confirm_msg)
        builder.setNegativeButton(R.string.delete_list_negative_button, null)
        builder.setPositiveButton(R.string.delete_list_positive_button) { dialog, which -> deleteList() }
        builder.create().show()
    }

    private fun deleteList() {
        scope.async {
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
        builder.setTitle(R.string.msg_sort_by)
        builder.setSingleChoiceItems(R.array.sort_by, sortBySelectedIndex) { dialog, which ->
            this.sortBySelectedIndex = which
            AppState.setString(SORT_BY_STATE_KEY, sortByKeyword)
            updateItemsList()
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun reverseSortOrder() {
        sortDescending = !sortDescending
        AppState.setBoolean(SORT_DESC_STATE_KEY, sortDescending)
        invalidateOptionsMenu()
        updateItemsList()
    }

    internal class ListItemAuthorComparator(descending: Boolean): Comparator<ListItem> {
        private val descending = descending

        override fun compare(o1: ListItem?, o2: ListItem?): Int {
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

    internal class ListItemPubdateComparator(descending: Boolean): Comparator<ListItem> {
        private val descending = descending

        override fun compare(o1: ListItem?, o2: ListItem?): Int {
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

    internal class ListItemTitleComparator(descending: Boolean): Comparator<ListItem> {
        private val descending = descending
        private val collator = Collator.getInstance()

        override fun compare(o1: ListItem?, o2: ListItem?): Int {
            val key1 = if (descending) o2?.record?.titleSort else o1?.record?.titleSort
            val key2 = if (descending) o1?.record?.titleSort else o2?.record?.titleSort
            return when {
                key1 == null && key2 == null -> 0
                key1 == null -> -1
                key2 == null -> 1
                else -> collator.compare(key1, key2) //key1.compareTo(key2)
            }
        }
    }

    internal inner class ListItemArrayAdapter(
        context: Context,
        private val resourceId: Int,
        items: ArrayList<ListItem>
    ) : ArrayAdapter<ListItem>(context, resourceId, items) {

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
            val pubdate = row.findViewById<View>(R.id.bookbagitem_pubdate) as TextView
            val remove = row.findViewById<View>(R.id.bookbagitem_remove_button) as Button

            val record = getItem(position)
            title.text = record?.record?.title
            author.text = record?.record?.author
            pubdate.text = record?.record?.pubdate
            remove.setOnClickListener(View.OnClickListener {
                val id = record?.id ?: return@OnClickListener
                scope.async {
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
