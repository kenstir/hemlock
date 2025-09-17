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

import net.kenstir.ui.AppState

// PersistableOption: generic interface for persisting options
interface PersistableOption<T> {
    val key: String

    // loads value from persistence if available, otherwise defaultValue
    fun load(): T

    // saves value to persistence
    fun save(value: T)
}

// SelectableOption: interface for selectable UI options
interface SelectableOption {
    val optionLabels: List<String>
    val optionValues: List<String>
    val optionIsEnabled: List<Boolean>
    val optionIsPrimary: List<Boolean>
}

// StringOption: combines persistence + selection
class StringOption(
    override val key: String,
    val title: String,
    val defaultValue: String,
    override val optionLabels: List<String>,
    override val optionValues: List<String> = emptyList(),
    override val optionIsEnabled: List<Boolean> = emptyList(),
    override val optionIsPrimary: List<Boolean> = emptyList()
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
        require(optionLabels.isNotEmpty()) { "optionLabels must not be empty" }
        require(optionValues.isEmpty() || optionValues.size == optionLabels.size)
        require(optionIsEnabled.isEmpty() || optionIsEnabled.size == optionLabels.size)
        require(optionIsPrimary.isEmpty() || optionIsPrimary.size == optionLabels.size)
        require(optionValues.contains(defaultValue) || optionLabels.contains(defaultValue))

        selectByValue(defaultValue)
    }

    constructor(
        key: String,
        title: String,
        defaultIndex: Int,
        optionLabels: List<String>,
        optionValues: List<String> = emptyList(),
        optionIsEnabled: List<Boolean> = emptyList(),
        optionIsPrimary: List<Boolean> = emptyList()
    ) : this(
        key,
        title,
        if (optionValues.isEmpty()) optionLabels[defaultIndex] else optionValues[defaultIndex],
        optionLabels,
        optionValues,
        optionIsEnabled,
        optionIsPrimary
    )

    fun selectByValue(selectedValue: String) {
        val values = if (optionValues.isEmpty()) optionLabels else optionValues
        selectedIndex = values.indexOfOrZero(selectedValue)
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
