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

package net.kenstir.hemlock.mock

import kotlinx.coroutines.test.runTest
import net.kenstir.hemlock.data.HemlockRecordRepository
import net.kenstir.hemlock.mock.MockMetadataSource.getRecordMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordRepositoryTest {
    @Test
    fun repository_getMetadata() = runTest {
        val repository = HemlockRecordRepository(MockRecordService())
        assertEquals(getRecordMetadata(1), repository.fetchMetadata(1).get())
    }
}
