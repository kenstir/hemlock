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
}
