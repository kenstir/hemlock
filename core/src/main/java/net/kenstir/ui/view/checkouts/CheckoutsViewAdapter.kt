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

package net.kenstir.ui.view.checkouts

import android.app.AlertDialog
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.kenstir.data.model.CircRecord
import net.kenstir.hemlock.R

class CheckoutsViewAdapter(
    private val items: List<CircRecord>,
    private val onRenewItem: (CircRecord) -> Unit,
) : RecyclerView.Adapter<CheckoutsViewAdapter.ViewHolder>() {

    class ViewHolder(
        view: View,
        private val onRenewItem: (CircRecord) -> Unit,
    ) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.checkout_record_title)
        val author: TextView = view.findViewById(R.id.checkout_record_author)
        val format: TextView = view.findViewById(R.id.checkout_record_format)
        val renewals: TextView = view.findViewById(R.id.checkout_record_renewals)
        val dueDate: TextView = view.findViewById(R.id.checkout_record_due_date)
        val renewButton: TextView = view.findViewById(R.id.renew_button)

        fun bindView(record: CircRecord) {
            title.text = record.title
            author.text = record.author
            format.text = record.record?.iconFormatLabel
            renewals.text =
                String.format(itemView.context.getString(R.string.checkout_renewals_left), record.renewals)
            dueDate.text = dueDateText(record)
            initRenewButton(record)
            maybeHighlightDueDate(record)
        }

        private fun dueDateText(record: CircRecord): String {
            return when {
                record.isOverdue ->
                    String.format(itemView.context.getString(R.string.label_due_date_overdue), record.dueDateLabel)

                record.isDueSoon && record.autoRenewals > 0 ->
                    String.format(itemView.context.getString(R.string.label_due_date_may_autorenew),
                        record.dueDateLabel)

                record.wasAutorenewed ->
                    String.format(itemView.context.getString(R.string.label_due_date_autorenewed),
                        record.dueDateLabel)

                else ->
                    String.format(itemView.context.getString(R.string.label_due_date), record.dueDateLabel)
            }
        }

        private fun maybeHighlightDueDate(record: CircRecord) {
            val style = when {
                record.isOverdue -> R.style.alertText
                record.isDueSoon -> R.style.warningText
                else -> R.style.HemlockText_ListTertiary
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dueDate?.setTextAppearance(style)
            } else {
                dueDate?.setTextAppearance(itemView.context, style)
            }
        }

        private fun initRenewButton(record: CircRecord) {
            val renewable = record.renewals > 0
            renewButton?.isEnabled = renewable
            renewButton?.setOnClickListener(View.OnClickListener {
                if (!renewable) return@OnClickListener
                val builder = AlertDialog.Builder(itemView.context)
                builder.setMessage(R.string.renew_item_message)
                builder.setNegativeButton(android.R.string.cancel, null)
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    //Analytics.logEvent("checkouts_renewitem", "num_renewals", record.renewals, "overdue", record.isOverdue)
                    onRenewItem(record)
                }
                builder.create().show()
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checkout_list_item, parent, false)
        return ViewHolder(view, onRenewItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int = items.size
}
