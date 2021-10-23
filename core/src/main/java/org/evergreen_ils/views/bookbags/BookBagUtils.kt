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

import android.app.AlertDialog
import android.widget.Toast
import androidx.core.os.bundleOf
import kotlinx.coroutines.async
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert

object BookBagUtils {
    fun showAddToListDialog(activity: BaseActivity, bookBags: List<BookBag>, info: RecordInfo) {
        val listNames = bookBags.map { it.name }.toTypedArray()

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.choose_list_message)
        builder.setItems(listNames) { _, which -> addRecordToList(activity, bookBags[which], info) }
        builder.create().show()
    }

    private fun addRecordToList(activity: BaseActivity, bookBag: BookBag, info: RecordInfo) {
        activity.async {
            val progress = ProgressDialogSupport()
            try {
                progress.show(activity, activity.getString(R.string.adding_to_list_message))

                val result = Gateway.actor.addItemToBookBagAsync(App.getAccount(), bookBag.id, info.doc_id)
                Analytics.logEvent(Analytics.Event.BOOKBAG_ADD_ITEM, bundleOf(
                    Analytics.Param.RESULT to Analytics.resultValue(result)
                ))
                when (result) {
                    is Result.Success -> {}
                    is Result.Error -> { activity.showAlert(result.exception); return@async }
                }

                Toast.makeText(activity, activity.resources.getString(R.string.msg_added_to_list), Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                activity.showAlert(ex)
            } finally {
                progress.dismiss()
            }
        }
    }
}
