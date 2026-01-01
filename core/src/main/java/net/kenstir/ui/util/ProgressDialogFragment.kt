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

package net.kenstir.ui.util

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ProgressDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(message: String?) = ProgressDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            isCancelable = false
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val density = resources.displayMetrics.density
        val progressSize = (density * 48).toInt()
        val padding = (density * 16).toInt()
        val gap = (density * 12).toInt()

        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(progressSize, progressSize).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(progressBar)
        }

        val message = arguments?.getString(ARG_MESSAGE)
        if (!message.isNullOrBlank()) {
            val tv = TextView(requireContext()).apply {
                text = message
                setPadding(gap, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            }
            container.addView(tv)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setView(container)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
}
