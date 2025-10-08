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

package net.kenstir.util

import net.kenstir.data.ShouldNotHappenException
import net.kenstir.ui.AppState

open class StringOption(
    override val key: String,
    open val defaultValue: String,
    override val optionLabels: List<String>,
    override val optionValues: List<String> = emptyList(),
) : PersistableOption<String>, SelectableOption {

    // index of selected option
    var selectedIndex: Int = 0
        private set

    // current value
    val value: String
        get() {
            val values = if (optionValues.isEmpty()) optionLabels else optionValues
            return values[selectedIndex]
        }

    // UI description = selected label
    val description: String
        get() = optionLabels[selectedIndex].trim()

    init {
        assert(optionLabels.isNotEmpty()) {
            val msg = "optionLabels must not be empty"
            Analytics.logException(ShouldNotHappenException(msg))
            msg
        }
        assert(optionValues.isEmpty() || optionValues.size == optionLabels.size) {
            val msg = "optionValues (${optionValues.count()}) must be empty or the same size as optionLabels (${optionLabels.count()})"
            Analytics.logException(ShouldNotHappenException(msg))
            msg
        }
        // Do not assert defaultValue is valid! The user's default org may be hidden.
        // See also test_invalidDefaultValue().
        //assert(optionValues.contains(defaultValue) || optionLabels.contains(defaultValue))

        selectByValue(defaultValue)
    }

    constructor(
        key: String,
        defaultIndex: Int,
        optionLabels: List<String>,
        optionValues: List<String> = emptyList(),
    ) : this(
        key,
        if (optionValues.isEmpty()) optionLabels[defaultIndex] else optionValues[defaultIndex],
        optionLabels,
        optionValues,
    )

    open fun selectByValue(newValue: String) {
        val values = if (optionValues.isEmpty()) optionLabels else optionValues
        selectedIndex = values.indexOfOrZero(newValue)
    }

    fun selectByIndex(index: Int) {
        assert(index >= 0 && index < optionLabels.size) {
            val msg = "index ($index) must be between 0 and ${optionLabels.size - 1}"
            Analytics.logException(ShouldNotHappenException(msg))
            msg
        }
        selectedIndex = index
    }

    override fun load(): String {
        val storedValue = AppState.getString(key, null)
        if (storedValue != null) {
            selectByValue(storedValue)
        } else {
            selectByValue(defaultValue)
        }
        return value
    }

    override fun save(value: String) {
        AppState.setString(key, value)
    }

    fun save() {
        save(value)
    }
}
