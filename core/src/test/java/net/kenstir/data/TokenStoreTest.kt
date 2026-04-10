/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package net.kenstir.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TokenStoreTest {
    val entry0: TokenEntry
    val entry1: TokenEntry
    val entry2: TokenEntry
    val entry3: TokenEntry
    val entry4: TokenEntry
    val entry5: TokenEntry

    val now = System.currentTimeMillis() / 1000
    val expiredTime = now - TokenStore.TOKEN_EXPIRATION_SECONDS - 60
    val needsRefreshTime = now - TokenStore.TOKEN_REFRESH_INTERVAL_SECONDS - 60

    init {
        // entry0 is expired, the others are current
        entry0 = TokenEntry("test-token-0", expiredTime)
        entry1 = TokenEntry("test-token-1", now - 86400 * 10)
        entry2 = TokenEntry("test-token-2", now - 86400 * 5)
        entry3 = TokenEntry("test-token-3", now - 86400 * 2)
        entry4 = TokenEntry("test-token-4", now - 86400)
        entry5 = TokenEntry("test-token-5", now)
    }

    @Test
    fun test_makeFromStringV2() {
        val json = """
            {
                "entries": [
                    {"token": "token-1", "added_at": 1775060400},
                    {"token": "token-2", "added_at": 1775060410}
                ]
            }
        """.replace("\\s".toRegex(), "")

        val ts = TokenStore.makeFromString(json)
        assertFalse(ts.isModified)
        assertEquals(2, ts.entries.size)
        assertEquals("token-1", ts.entries[0].token)
        assertEquals(1775060400, ts.entries[0].addedAt)

        val str = ts.encodeToString()
        assertEquals(json, str)
    }

    @Test
    fun test_makeFromStringV1() {
        val pushNotificationData = "old-v1-token"

        val ts = TokenStore.makeFromString(pushNotificationData)
        assertTrue(ts.isModified)
        assertEquals(1, ts.entries.size)
        assertEquals("old-v1-token", ts.currentToken)
        assertTrue(abs(ts.entries[0].addedAt) - now < 10)
    }

    @Test
    fun test_addCurrentToken() {
        val ts = TokenStore()
        ts.entries.addAll(listOf(entry1, entry2, entry3, entry4))
        assertFalse(ts.isModified)

        // addTokenAsCurrent with a new token pushes out the oldest token
        ts.addCurrentToken("new-token-5")
        assertTrue(ts.isModified)
        assertEquals(TokenStore.MAX_TOKEN_ENTRIES, ts.entries.size)
        val entry = ts.entries.last()
        assertEquals("new-token-5", entry.token)
        assertEquals("new-token-5", ts.currentToken)

        // addCurrentToken with an existing token within the refresh interval
        // leaves it in place and does not update timestamp
        val indexBefore = ts.entries.indexOfFirst { it.token == entry3.token }
        assertNotEquals(-1, indexBefore)
        ts.addCurrentToken("test-token-3")
        val indexAfter = ts.entries.indexOfFirst { it.token == entry3.token }
        assertEquals(indexBefore, indexAfter)
        assertEquals(TokenStore.MAX_TOKEN_ENTRIES, ts.entries.size)
        assertEquals(entry3.addedAt, ts.entries[indexAfter].addedAt)
    }

    @Test
    fun test_addCurrent_Token_withRefresh() {
        val ts = TokenStore()
        ts.entries.add(TokenEntry("token-1", needsRefreshTime))
        ts.entries.add(TokenEntry("token-2", now - 60))
        assertFalse(ts.isModified)

        // addCurrentToken with a fresh token leaves the TS unmodified
        ts.addCurrentToken("token-2")
        assertFalse(ts.isModified)
        assertEquals(2, ts.entries.size)
        assertEquals("token-2", ts.entries[1].token)

        // addCurrentToken with a token needing refresh moves it to the end with a new timestamp
        ts.addCurrentToken("token-1")
        assertTrue(ts.isModified)
        assertEquals(2, ts.entries.size)
        assertEquals("token-1", ts.entries[1].token)
    }
}
