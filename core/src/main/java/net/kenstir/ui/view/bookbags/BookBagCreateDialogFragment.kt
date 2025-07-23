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

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import net.kenstir.hemlock.R

class BookBagCreateDialogFragment : DialogFragment() {

    // Interface to communicate back to the Activity/Fragment
    interface CreateListener {
        fun onBookBagCreated(name: String, description: String)
    }

    private var listener: CreateListener? = null
    private lateinit var nameLayout: TextInputLayout
    private lateinit var nameText: EditText
    private lateinit var descriptionText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Ensure the hosting Activity/Fragment implements the listener
        try {
            listener = targetFragment as? CreateListener ?: context as? CreateListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                (targetFragment?.toString() ?: context.toString()) +
                        " must implement BookBagCreateDialogFragment.CreateListener"
            )
        }

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_create_bookbag, null)

        nameLayout = view.findViewById(R.id.list_name_layout)
        nameText = view.findViewById(R.id.list_name_edit_text)
        descriptionText = view.findViewById(R.id.list_description_edit_text)

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(view)
            .setTitle(R.string.create_list_title)
            .setPositiveButton(R.string.create_list_title, null) // Set to null to override and prevent auto-dismiss
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

        val dialog = builder.create()

        // Override the positive button click to handle validation before dismissing
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameText.text.toString().trim()
                val description = descriptionText.text.toString().trim()

                if (name.isEmpty()) {
                    nameLayout.error = getString(R.string.error_list_name_empty)
                } else {
                    nameLayout.error = null // Clear error
                    listener?.onBookBagCreated(name, description)
                    dismiss() // Dismiss the dialog only if validation passes
                }
            }
        }
        return dialog
    }

    companion object {
        const val TAG = "BookBagCreateDialogFragment"
    }
}
