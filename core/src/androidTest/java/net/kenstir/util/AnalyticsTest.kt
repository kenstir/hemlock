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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTest {

    @Test
    fun test_Regex() {
        val authCompleteResponse = """
            {"payload":[{"ilsevent":0,"textcode":"SUCCESS","desc":"Success","pid":93666,"stacktrace":"oils_auth.c:636","payload":{"authtoken":"7983cdc073e9a85aa51e2754699d04ce","authtime":1209600}}],"status":200}
        """.trimIndent()
        assertTrue(Analytics.mRedactedResponseRegex.containsMatchIn(authCompleteResponse))

        val authSessionRetrieveResponse = """
            
        """.trimIndent()
    }

    @Test
    fun test_searchTextStats() {
        val searchText = "La la land"
        val b = Analytics.searchTextStats(searchText)
        assertEquals(2, b.size())
        assertEquals(2, b.getInt(Analytics.Param.SEARCH_TERM_UNIQ_WORDS))
        assertEquals(30, b.getInt(Analytics.Param.SEARCH_TERM_AVG_WORD_LEN_X10))
    }
}
