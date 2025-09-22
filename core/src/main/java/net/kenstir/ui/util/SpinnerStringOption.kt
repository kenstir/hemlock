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

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import net.kenstir.util.StringOption

class SpinnerStringOption(
    key: String,
    defaultValue: String,
    optionLabels: List<String>,
    optionValues: List<String> = emptyList()
) : StringOption(key, defaultValue, optionLabels, optionValues) {
    /// because Spinner onItemSelected is called on initialization, we need to squelch the first change
    /// and any subsequent programmatic changes
    var squelchNextChange = false

    var spinner: Spinner? = null

    override fun load(): String {
        val value = super.load()
        spinner?.setSelection(selectedIndex)
        return value
    }

    override fun selectByValue(newValue: String) {
        super.selectByValue(newValue)
        squelchNextChange = true
        spinner?.setSelection(selectedIndex)
    }

    fun addSelectionListener(listener: (Int, String) -> Unit) {
        squelchNextChange = true // ignore first automatic onItemSelected callback
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (squelchNextChange) {
                    squelchNextChange = false
                    return
                }
                if (position != selectedIndex) {
                    selectByIndex(position)
                    save()
                    listener(position, value)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }
    }
}
