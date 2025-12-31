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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
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
        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
            val size = (resources.displayMetrics.density * 48).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        val container = FrameLayout(requireContext()).apply {
            val pad = (resources.displayMetrics.density * 20).toInt()
            setPadding(pad, pad, pad, pad)
            addView(progressBar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val message = arguments?.getString(ARG_MESSAGE)
        val builder = AlertDialog.Builder(requireContext())
            .setView(container)

        if (!message.isNullOrBlank()) builder.setMessage(message)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
}
