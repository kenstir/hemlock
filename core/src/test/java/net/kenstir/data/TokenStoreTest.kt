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

import net.kenstir.util.decodeFromBase64URL
import net.kenstir.util.encodeToBase64URL
import net.kenstir.util.trimAllWhitespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TokenStoreTest {
    val now = System.currentTimeMillis() / 1000
    val expiredTime = now - TokenStore.TOKEN_EXPIRATION_SECONDS - 60
    val needsRefreshTime = now - TokenStore.TOKEN_REFRESH_INTERVAL_SECONDS - 60

    val entry1 = TokenEntry("test-token-1", now - 86400 * 10)
    val entry2 = TokenEntry("test-token-2", now - 86400 * 5)
    val entry3 = TokenEntry("test-token-3", now - 86400 * 2)
    val entry4 = TokenEntry("test-token-4", now - 86400)

    @Test
    fun test_initFromString_v1() {
        val pushNotificationData = "old-v1-token"

        val ts = TokenStore()
        ts.initFromString(pushNotificationData)
        assertTrue(ts.isModified)
        assertEquals(1, ts.entries.size)
        assertEquals("old-v1-token", ts.entries[0].token)
        assertTrue(abs(ts.entries[0].addedAt - now) < 2)
    }

    @Test
    fun test_initFromString_v1LooksLikeV2() {
        val pushNotificationData = TokenStore.V2_ENCODED_TOKEN_PREFIX + "old-v1-token"

        val ts = TokenStore()
        ts.initFromString(pushNotificationData)
        assertTrue(ts.isModified)
        assertEquals(1, ts.entries.size)
        assertEquals(pushNotificationData, ts.entries[0].token)
        assertTrue(abs(ts.entries[0].addedAt - now) < 2)
    }

    @Test
    fun test_initFromString_empty() {
        val testData = listOf(null, "")
        for (pushNotificationData in testData) {
            val ts = TokenStore()
            ts.initFromString(pushNotificationData)
            assertFalse(ts.isModified)
            assertTrue(ts.entries.isEmpty())
        }
    }

    @Test
    fun test_encodingIsCompatible() {
        // Check that the implementation we are using is compatible with other implementations,
        // that is, base64-url-encoding with no padding.
        val json = """{"a":"??~"}"""
        val want = "eyJhIjoiPz9-In0" // plain base64 would be "eyJhIjoiPz9+In0="

        val encoded = json.encodeToBase64URL()
        assertEquals(want, encoded)

        val decoded = encoded.decodeFromBase64URL()
        assertEquals(json, decoded)
    }

    @Test
    fun test_initFromString_v2() {
        val json = """
            {
                "entries": [
                    {"token": "token-1", "added_at": 1775060400},
                    {"token": "token-2", "added_at": 1775060410}
                ]
            }
        """.trimAllWhitespace()
        val encoded = json.encodeToBase64URL()

        val ts = TokenStore()
        ts.initFromString(encoded)
        assertFalse(ts.isModified)
        assertEquals(2, ts.entries.size)
        assertEquals("token-1", ts.entries[0].token)
        assertEquals(1775060400, ts.entries[0].addedAt)

        // check that encoding the store produces the original string (ignoring whitespace)
        // NB: kotlinx.serialization adds keys in the order they appear in the class
        val str = ts.encodeToString()
        assertEquals(encoded, str)
    }

    @Test
    fun test_initFromString_forwardCompatible() {
        // v2 format with possible future additions
        // NB: "entries" appears first
        val json = """
            {
                "entries": [
                    {"token": "token-1", "added_at": 1775060400, "extra_field": "ignored"},
                    {"token": "token-2", "added_at": 1775060410, "extra_field": "ignored"}
                ],
                "version": 3
            }
        """.trimAllWhitespace()
        val encoded = json.encodeToBase64URL()

        // check that we ignore unknown fields and parse the known ones correctly
        val ts = TokenStore()
        ts.initFromString(encoded)
        assertFalse(ts.isModified)
        assertEquals(2, ts.entries.size)
        assertEquals("token-1", ts.entries[0].token)
        assertEquals(1775060400, ts.entries[0].addedAt)
    }

    @Test
    fun test_initFromString_removesExpiredToken() {
        val json = """
            {
                "entries": [
                    {"token": "token-1", "added_at": ${expiredTime}},
                    {"token": "token-2", "added_at": ${now}}
                ]
            }
        """.trimAllWhitespace()
        val encoded = json.encodeToBase64URL()

        val ts = TokenStore()
        ts.initFromString(encoded)
        assertTrue(ts.isModified)
        assertEquals(1, ts.entries.size)
        assertEquals("token-2", ts.entries[0].token)
    }

    @Test
    fun test_addCurrentToken_overflow() {
        val ts = TokenStore()
        ts.entries.addAll(listOf(entry1, entry2, entry3, entry4))
        assertFalse(ts.isModified)

        // adding a new token exceeds the max size, removing the oldest token
        ts.addCurrentToken("new-token-5")
        assertTrue(ts.isModified)
        assertEquals(TokenStore.MAX_TOKEN_ENTRIES, ts.entries.size)
        val entry = ts.entries.last()
        assertEquals("new-token-5", entry.token)
    }

    @Test
    fun test_addCurrentToken_noRefresh() {
        val ts = TokenStore()
        ts.entries.addAll(listOf(entry1, entry2, entry3, entry4))
        assertFalse(ts.isModified)

        // adding an existing token within the refresh interval
        // leaves it in place and does not update its timestamp
        val indexBefore = ts.entries.indexOfFirst { it.token == entry3.token }
        assertNotEquals(-1, indexBefore)
        ts.addCurrentToken("test-token-3")
        assertFalse(ts.isModified)
        val indexAfter = ts.entries.indexOfFirst { it.token == entry3.token }
        assertEquals(indexBefore, indexAfter)
        assertEquals(TokenStore.MAX_TOKEN_ENTRIES, ts.entries.size)
        assertEquals(entry3.addedAt, ts.entries[indexAfter].addedAt)
    }

    @Test
    fun test_addCurrentToken_withRefresh() {
        val ts = TokenStore()
        ts.entries.add(TokenEntry("token-1", needsRefreshTime))
        ts.entries.add(TokenEntry("token-2", now - 60))
        assertFalse(ts.isModified)

        // adding a token needing refresh moves it to the end with a new timestamp
        ts.addCurrentToken("token-1")
        assertTrue(ts.isModified)
        assertEquals(2, ts.entries.size)
        val indexAfter = ts.entries.indexOfFirst { it.token == "token-1" }
        assertEquals(1, indexAfter)
        assertTrue(ts.entries[indexAfter].addedAt > needsRefreshTime)
    }
}
