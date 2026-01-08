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
package net.kenstir.ui.view.search

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import net.kenstir.data.model.BibRecord
import net.kenstir.ui.Key

object RecordDetails {
    fun launchDetailsFlow(context: Context, records: ArrayList<BibRecord>, position: Int) {
        // discard boundary cases early
        if (records.isEmpty()) return
        if (position < 0 || position >= records.size) return

        // determine name of parent activity
        var parentActivityLabel: String? = null
        try {
            val info = context.packageManager.getActivityInfo(ComponentName(context, context.javaClass.getCanonicalName()!!), 0)
            parentActivityLabel = context.getString(info.labelRes)
        } catch (_: PackageManager.NameNotFoundException) {
        }

        // Prevent TransactionTooLargeException by limiting the data passed via Intent.
        // In my testing, 100 records ~= 100KB, well below the limit of ~500KB.  If the
        // list is too long, start the details flow with just the selected item.
        val limit = 100
        val (launchRecords, launchPosition) = when {
            records.size > limit -> {
                arrayListOf(records[position]) to 0
            }
            else -> {
                records to position
            }
        }

        // launch RecordDetailsActivity
        val intent = Intent(context, RecordDetailsActivity::class.java)
        intent.putExtra(Key.RECORD_LIST, launchRecords)
        intent.putExtra(Key.RECORD_POSITION, launchPosition)
        parentActivityLabel?.let { intent.putExtra(Key.TITLE, it) }
        context.startActivity(intent)
    }
}
