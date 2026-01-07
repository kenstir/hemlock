/*
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

import android.app.AlertDialog
import android.widget.Toast
import androidx.core.os.bundleOf
import kotlinx.coroutines.async
import net.kenstir.hemlock.R
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.PatronList
import net.kenstir.util.Analytics
import net.kenstir.ui.App
import net.kenstir.ui.Appx
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.showAlert

object BookBagUtils {
    fun showAddToListDialog(activity: BaseActivity, bookBags: List<PatronList>, info: BibRecord) {
        val listNames = bookBags.map { it.name }.toTypedArray()

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.choose_list_message)
        builder.setItems(listNames) { _, which -> addRecordToList(activity, bookBags[which], info) }
        builder.create().show()
    }

    private fun addRecordToList(activity: BaseActivity, bookBag: PatronList, info: BibRecord) {
        activity.scope.async {
            try {
                activity.showBusy(R.string.adding_to_list_message)

                val result = Appx.svc.userService.addItemToPatronList(
                    App.getAccount(),
                    bookBag.id,
                    info.id
                )
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
                activity.hideBusy()
            }
        }
    }
}
