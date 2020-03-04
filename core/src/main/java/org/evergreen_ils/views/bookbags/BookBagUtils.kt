/*
 * Copyright (C) 2016 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.evergreen_ils.views.bookbags

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.accountAccess.SessionNotFoundException
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.searchCatalog.RecordInfo

object BookBagUtils {
    private val TAG = BookBagUtils::class.java.simpleName
    @JvmStatic
    fun showAddToListDialog(activity: Activity, bookBags: List<BookBag>, info: RecordInfo) {
        val list_names = arrayOfNulls<String>(bookBags.size)
        for (i in list_names.indices) list_names[i] = bookBags[i].name
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.choose_list_message)
        builder.setItems(list_names) { dialog, which -> addRecordToList(activity, bookBags[which], info) }
        builder.create().show()
    }

    fun addRecordToList(activity: Activity, bookbag: BookBag, info: RecordInfo) {
        val progressDialog = ProgressDialog.show(activity,
                activity.getString(R.string.dialog_please_wait),
                activity.getString(R.string.adding_to_list_message))
        val thread = Thread(Runnable {
            val ac = AccountAccess.getInstance()
            try {
                ac.addRecordToBookBag(info.doc_id, bookbag.id)
            } catch (e: SessionNotFoundException) {
                Log.d(TAG, "caught", e)
            }
            activity.runOnUiThread { progressDialog.dismiss() }
        })
        thread.start()
    }
}
