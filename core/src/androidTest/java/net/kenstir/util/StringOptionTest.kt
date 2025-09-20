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
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class StringOptionTest {

    @Test
    fun test_initWithLabelsOnly() {
        val option = StringOption(
            key = "test_option",
            defaultValue = "Bravo",
            optionLabels = listOf("Alpha", "Bravo", "Charlie")
        )

        assertEquals("Bravo", option.value)
        option.save()

        option.selectByValue("Charlie")
        assertEquals("Charlie", option.value)

        option.load()
        assertEquals("Bravo", option.value)
    }

    @Test
    fun test_initWithLabelsAndValues() {
        val option = StringOption(
            key = "test_option",
            defaultValue = "B",
            optionLabels = listOf("Alpha", "Bravo", "Charlie"),
            optionValues = listOf("A", "B", "C")
        )

        assertEquals("B", option.value)
        option.save()

        option.selectByValue("C")
        assertEquals("C", option.value)

        option.load()
        assertEquals("B", option.value)
    }

    @Test
    fun test_initByIndex() {
        val option = StringOption(
            key = "test_option",
            defaultIndex = 2,
            optionLabels = listOf("Alpha", "Bravo", "Charlie"),
            optionValues = listOf("A", "B", "C")
        )
        assertEquals("C", option.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_invalidDefaultValue() {
        StringOption(
            key = "test_option",
            defaultValue = "Delta",
            optionLabels = listOf("Alpha", "Bravo", "Charlie"),
            optionValues = listOf("A", "B", "C")
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun test_invalidDefaultIndex() {
        val option = StringOption(
            key = "test_option",
            defaultIndex = 5,
            optionLabels = listOf("Alpha", "Bravo", "Charlie"),
            optionValues = listOf("A", "B", "C")
        )
    }

    @Test
    fun test_obsoleteSavedValue() {
        val option = StringOption(
            key = "test_option",
            defaultValue = "Bravo",
            optionLabels = listOf("Alpha", "Bravo", "Charlie"),
            optionValues = listOf("A", "B", "C")
        )

        // Simulate what would happen if a saved value is removed from the available options
        option.save("OBSOLETE")

        // Loading should default to the first value
        option.load()
        assertEquals("A", option.value)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            AppState.clearTestPreferences()
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            AppState.clearTestPreferences()
        }
    }
}
